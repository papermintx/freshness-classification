package com.skripsi.cnnfreshscan.presentation.viewmodel

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skripsi.cnnfreshscan.navigation.IMAGE_URI_ARG
import com.skripsi.cnnfreshscan.presentation.util.DEFAULT_PREDICTION_LABEL
import com.skripsi.cnnfreshscan.presentation.util.buildHandlingAdvice
import com.skripsi.cnnfreshscan.presentation.util.buildWatermarkText
import com.skripsi.cnnfreshscan.presentation.util.rotateClockwise
import com.skripsi.cnnfreshscan.presentation.util.toDisplayLabel
import com.skripsi.core.domain.usecase.AnalyzeProduceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.system.measureNanoTime

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val analyzeProduceUseCase: AnalyzeProduceUseCase,
    savedStateHandle: SavedStateHandle,
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResultUiState(isLoading = true))
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    private val imageUri: Uri? = savedStateHandle.get<String>(IMAGE_URI_ARG)
        ?.let(Uri::decode)
        ?.takeIf { it.isNotBlank() }
        ?.let(Uri::parse)

    init {
        analyzeSelectedImage()
    }

    fun saveToGallery() {
        val currentState = _uiState.value
        val bitmap = currentState.imageBitmap ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                withContext(Dispatchers.IO) {
                    saveBitmapToGallery(
                        bitmap = bitmap,
                        watermarkText = buildWatermarkText(
                            currentState.displayLabel,
                            currentState.confidence
                        )
                    )
                }

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        snackbarMessage = "Gambar berhasil disimpan ke galeri"
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = exception.localizedMessage ?: "Gagal menyimpan gambar ke galeri",
                        snackbarMessage = "Gagal menyimpan gambar"
                    )
                }
            }
        }
    }

    fun consumeSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun analyzeSelectedImage() {
        val sourceUri = imageUri
        if (sourceUri == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = null,
                    snackbarMessage = INVALID_INPUT_MESSAGE
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val bitmap = withContext(Dispatchers.IO) { loadBitmap(sourceUri) }
                val analysisResult = withContext(Dispatchers.Default) {
                    var bestResultName: String? = null
                    var bestResultConfidence = 0f

                    val predictionTimeMs = measureNanoTime {
                        val bestResult = analyzeProduceUseCase(bitmap)
                            .maxByOrNull { result -> result.accuracyScore }
                        bestResultName = bestResult?.name
                        bestResultConfidence = bestResult?.accuracyScore ?: 0f
                    } / 1_000_000L

                    ImageAnalysisResult(
                        label = bestResultName,
                        confidence = bestResultConfidence,
                        predictionTimeMs = predictionTimeMs
                    )
                }

                val confidence = analysisResult.confidence
                val rawLabel = analysisResult.label
                val displayLabel = toDisplayLabel(rawLabel, confidence)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        imageBitmap = bitmap,
                        displayLabel = displayLabel,
                        confidence = confidence,
                        predictionTimeMs = analysisResult.predictionTimeMs,
                        handlingAdvice = buildHandlingAdvice(rawLabel, confidence),
                        scannedAt = formatScanTimestamp(System.currentTimeMillis()),
                        error = null
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                        snackbarMessage = INVALID_INPUT_MESSAGE,
                        displayLabel = DEFAULT_PREDICTION_LABEL,
                        confidence = 0f,
                        predictionTimeMs = null,
                        scannedAt = null
                    )
                }
            }
        }
    }

    private fun loadBitmap(uri: Uri): Bitmap {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val inputStream = when (uri.scheme) {
            ContentResolver.SCHEME_FILE -> {
                val filePath = uri.path ?: throw IllegalArgumentException("Path file tidak valid")
                FileInputStream(File(filePath))
            }
            else -> appContext.contentResolver.openInputStream(uri)
        } ?: throw IllegalStateException("Tidak dapat membuka gambar")

        val decodedBitmap = inputStream.use {
            BitmapFactory.decodeStream(it, null, options)
                ?: throw IllegalStateException("Bitmap tidak dapat dimuat")
        }

        return decodedBitmap.rotateClockwise(readImageRotationDegrees(uri))
    }

    private fun readImageRotationDegrees(uri: Uri): Int {
        val exifInterface = when (uri.scheme) {
            ContentResolver.SCHEME_FILE -> {
                val filePath = uri.path ?: return 0
                ExifInterface(filePath)
            }
            else -> {
                appContext.contentResolver.openInputStream(uri)?.use { inputStream ->
                    ExifInterface(inputStream)
                } ?: return 0
            }
        }

        return when (
            exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        ) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    private fun saveBitmapToGallery(bitmap: Bitmap, watermarkText: String) {
        val watermarkedBitmap = createWatermarkedBitmap(bitmap, watermarkText)
        val resolver = appContext.contentResolver
        val fileName = "SegarCek_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/SegarCek"
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("Gagal membuat file gambar baru")

        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                if (!watermarkedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
                    throw IllegalStateException("Bitmap tidak berhasil disimpan")
                }
            } ?: throw IllegalStateException("Output stream galeri tidak tersedia")

            val publishValues = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            resolver.update(uri, publishValues, null, null)
        } catch (exception: Exception) {
            resolver.delete(uri, null, null)
            throw exception
        }
    }

    private fun createWatermarkedBitmap(source: Bitmap, watermarkText: String): Bitmap {
        val mutableBitmap = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val padding = mutableBitmap.width * 0.04f
        val textSize = (mutableBitmap.width * 0.045f).coerceIn(28f, 64f)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            this.textSize = textSize
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            setShadowLayer(8f, 0f, 0f, android.graphics.Color.BLACK)
        }

        val bounds = Rect()
        textPaint.getTextBounds(watermarkText, 0, watermarkText.length, bounds)

        val textX = mutableBitmap.width - padding - bounds.width()
        val textY = mutableBitmap.height - padding

        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(150, 0, 0, 0)
        }

        val backgroundRect = RectF(
            textX - padding / 2f,
            textY - bounds.height() - padding / 2f,
            mutableBitmap.width - padding / 2f,
            textY + padding / 3f
        )

        canvas.drawRoundRect(backgroundRect, 18f, 18f, backgroundPaint)
        canvas.drawText(watermarkText, textX, textY, textPaint)
        return mutableBitmap
    }

    private fun formatScanTimestamp(timestamp: Long): String {
        val locale = Locale.forLanguageTag("id-ID")
        return SimpleDateFormat("dd MMM yyyy, HH:mm", locale).format(Date(timestamp))
    }

    private companion object {
        const val INVALID_INPUT_MESSAGE = "Input tidak valid. Silakan pilih gambar lain."
    }

}

private data class ImageAnalysisResult(
    val label: String?,
    val confidence: Float,
    val predictionTimeMs: Long
)

data class ResultUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val imageBitmap: Bitmap? = null,
    val displayLabel: String = DEFAULT_PREDICTION_LABEL,
    val confidence: Float = 0f,
    val predictionTimeMs: Long? = null,
    val scannedAt: String? = null,
    val handlingAdvice: String = "",
    val error: String? = null,
    val snackbarMessage: String? = null
)
