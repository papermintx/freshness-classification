package com.skripsi.core.data.benchmark

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import com.skripsi.core.data.image.ImagePreprocessor
import com.skripsi.core.data.image.RoiCropper
import com.skripsi.core.domain.model.CameraRoiBenchmarkSummary
import com.skripsi.core.domain.model.CameraRoiInferenceResult
import com.skripsi.tflite_engine.ProduceClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import androidx.core.graphics.createBitmap

class CameraRoiBenchmarkRunner @Inject constructor(
    private val context: Context,
    private val classifier: ProduceClassifier,
    private val roiCropper: RoiCropper,
    private val imagePreprocessor: ImagePreprocessor
) {
    suspend fun warmUp(iterations: Int = DEFAULT_WARM_UP_ITERATIONS) = withContext(Dispatchers.Default) {
        val initialized = classifier.initialize()
        if (!initialized) {
            throw IllegalStateException("Gagal menjalankan model TensorFlow Lite.")
        }

        val warmUpBitmap =
            createBitmap(ImagePreprocessor.INPUT_WIDTH, ImagePreprocessor.INPUT_HEIGHT).apply {
            eraseColor(android.graphics.Color.rgb(127, 127, 127))
        }

        repeat(iterations.coerceAtLeast(DEFAULT_WARM_UP_ITERATIONS)) {
            val tensorImage = imagePreprocessor.preprocess(warmUpBitmap)
            classifier.classifyFromTensor(tensorImage)
        }
    }

    suspend fun runSingleCapture(
        sessionId: String,
        captureIndex: Int,
        actualLabel: String,
        distanceCm: Int?,
        intervalSincePreviousInferenceMs: Double,
        cameraLensFacing: String,
        cameraFpsRange: String,
        cameraMaxFps: Int,
        cameraMegapixels: Double,
        cameraResolution: String,
        bitmap: Bitmap,
        saveRoiImage: Boolean = true
    ): CameraRoiInferenceResult = withContext(Dispatchers.Default) {
        val totalStart = SystemClock.elapsedRealtimeNanos()

        val cropStart = SystemClock.elapsedRealtimeNanos()
        val roiBitmap = roiCropper.cropCenterSquare(bitmap)
        val roiCropTimeMs = nanosToMs(SystemClock.elapsedRealtimeNanos() - cropStart)

        val savedPath = if (saveRoiImage) saveRoiImage(sessionId, captureIndex, roiBitmap) else null

        val preprocessingStart = SystemClock.elapsedRealtimeNanos()
        val tensorImage = imagePreprocessor.preprocess(roiBitmap)
        val preprocessingTimeMs = nanosToMs(SystemClock.elapsedRealtimeNanos() - preprocessingStart)

        val inferenceStart = SystemClock.elapsedRealtimeNanos()
        val predictions = classifier.classifyFromTensor(tensorImage)
        val inferenceTimeMs = nanosToMs(SystemClock.elapsedRealtimeNanos() - inferenceStart)
        val totalTimeMs = nanosToMs(SystemClock.elapsedRealtimeNanos() - totalStart)

        val topPrediction = predictions.firstOrNull()
        val predictedLabel = topPrediction?.label.orEmpty()

        CameraRoiInferenceResult(
            timestamp = Instant.now().toString(),
            sessionId = sessionId,
            deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            modelName = classifier.modelName,
            modelFormat = classifier.modelFormat,
            captureIndex = captureIndex,
            distanceCm = distanceCm,
            sourceImageWidth = bitmap.width,
            sourceImageHeight = bitmap.height,
            roiImageWidth = roiBitmap.width,
            roiImageHeight = roiBitmap.height,
            modelInputWidth = ImagePreprocessor.INPUT_WIDTH,
            modelInputHeight = ImagePreprocessor.INPUT_HEIGHT,
            intervalSincePreviousInferenceMs = intervalSincePreviousInferenceMs,
            cameraLensFacing = cameraLensFacing,
            cameraFpsRange = cameraFpsRange,
            cameraMaxFps = cameraMaxFps,
            cameraMegapixels = cameraMegapixels,
            cameraResolution = cameraResolution,
            actualLabel = actualLabel,
            predictedLabel = predictedLabel,
            confidence = topPrediction?.confidence ?: 0f,
            isCorrect = predictedLabel == actualLabel,
            roiCropTimeMs = roiCropTimeMs,
            preprocessingTimeMs = preprocessingTimeMs,
            inferenceTimeMs = inferenceTimeMs,
            totalTimeMs = totalTimeMs,
            savedRoiImagePath = savedPath
        )
    }

    fun summarize(
        sessionId: String,
        actualLabel: String,
        distanceCm: Int?,
        results: List<CameraRoiInferenceResult>
    ): CameraRoiBenchmarkSummary {
        return BenchmarkSummaryCalculator.summarizeCameraRoi(sessionId, actualLabel, distanceCm, results)
    }

    private fun saveRoiImage(sessionId: String, captureIndex: Int, bitmap: Bitmap): String? {
        val fileName = "${sessionId}_roi_${captureIndex.toString().padStart(4, '0')}.jpg"
        val relativePath = "${android.os.Environment.DIRECTORY_DOWNLOADS}/CnnFreshScanResearch/research_roi_captures"
        return runCatching {
            val resolver = context.contentResolver
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return null

            resolver.openOutputStream(uri)?.use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)
            }

            values.clear()
            values.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            
            "Downloads/CnnFreshScanResearch/research_roi_captures/$fileName"
        }.getOrNull()
    }

    companion object {
        const val DEFAULT_WARM_UP_ITERATIONS = 5
        const val ROI_CAPTURE_DIR = "research_roi_captures"

        private fun nanosToMs(nanos: Long): Double = nanos / 1_000_000.0
    }
}
