package com.skripsi.cnnfreshscan.presentation.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skripsi.cnnfreshscan.presentation.util.DEFAULT_PREDICTION_LABEL
import com.skripsi.cnnfreshscan.presentation.util.rotateClockwise
import com.skripsi.cnnfreshscan.presentation.util.toDisplayLabel
import com.skripsi.core.domain.usecase.AnalyzeProduceUseCase
import com.skripsi.core.domain.usecase.GetRoiConfigurationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.system.measureNanoTime
import javax.inject.Inject

private const val TAG = "CameraViewModel"
private const val MIN_ANALYSIS_INTERVAL_MS = 70L
private const val MAX_ANALYSIS_INTERVAL_MS = 180L
private const val REALTIME_VOTE_WINDOW_SIZE = 20
private const val MIN_REALTIME_VOTES_BEFORE_DISPLAY = 5

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val analyzeProduceUseCase: AnalyzeProduceUseCase,
    getRoiConfigurationUseCase: GetRoiConfigurationUseCase
) : ViewModel() {

    private val roiConfiguration = getRoiConfigurationUseCase()
    private val _uiState = MutableStateFlow(
        CameraUiState(
            roiSizeFraction = roiConfiguration.sizeFraction,
            roiVerticalBias = roiConfiguration.verticalBias
        )
    )
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var lastAnalysisTime: Long = 0L
    private var adaptiveAnalysisIntervalMs: Long = MIN_ANALYSIS_INTERVAL_MS
    @Volatile
    private var latestAnalyzedBitmap: Bitmap? = null
    @Volatile
    private var isAnalyzingFrame: Boolean = false
    private val livePredictionVotes = ArrayDeque<LivePredictionVote>()

    fun processImageProxy(imageProxy: ImageProxy) {
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val frameWidth = if (rotationDegrees % 180 == 0) imageProxy.width else imageProxy.height
        val frameHeight = if (rotationDegrees % 180 == 0) imageProxy.height else imageProxy.width

        if (_uiState.value.analysisFrameWidth != frameWidth || _uiState.value.analysisFrameHeight != frameHeight) {
            _uiState.update {
                it.copy(
                    analysisFrameWidth = frameWidth,
                    analysisFrameHeight = frameHeight
                )
            }
        }

        val currentTime = System.currentTimeMillis()
        if (isAnalyzingFrame || currentTime - lastAnalysisTime < adaptiveAnalysisIntervalMs) {
            imageProxy.close()
            return
        }

        val bitmap = try {
            imageProxy.toBitmap()
        } catch (exception: Exception) {
            Log.e(TAG, "Bitmap conversion failed", exception)
            imageProxy.close()
            return
        }

        imageProxy.close()
        lastAnalysisTime = currentTime
        isAnalyzingFrame = true

        viewModelScope.launch {
            try {
                val analysisResult = withContext(Dispatchers.Default) {
                    val rotatedBitmap = bitmap.rotateClockwise(rotationDegrees)
                    latestAnalyzedBitmap = rotatedBitmap
                    var bestResultName: String? = null
                    var bestResultConfidence = 0f

                    val predictionTimeMs = measureNanoTime {
                        // Classifier already sorts descending; ambil elemen pertama.
                        val bestResult = analyzeProduceUseCase(rotatedBitmap).firstOrNull()
                        bestResultName = bestResult?.name
                        bestResultConfidence = bestResult?.accuracyScore ?: 0f
                    } / 1_000_000L

                    LiveAnalysisResult(
                        frameWidth = rotatedBitmap.width,
                        frameHeight = rotatedBitmap.height,
                        bestLabel = bestResultName,
                        bestConfidence = bestResultConfidence,
                        predictionTimeMs = predictionTimeMs
                    )
                }

                adaptiveAnalysisIntervalMs = (analysisResult.predictionTimeMs + 80L)
                    .coerceIn(MIN_ANALYSIS_INTERVAL_MS, MAX_ANALYSIS_INTERVAL_MS)

                val stablePrediction = updateStableRealtimePrediction(analysisResult)

                _uiState.update {
                    it.copy(
                        analysisFrameWidth = analysisResult.frameWidth,
                        analysisFrameHeight = analysisResult.frameHeight,
                        livePredictionLabel = stablePrediction?.let { prediction ->
                            toDisplayLabel(prediction.label, prediction.confidence)
                        } ?: it.livePredictionLabel,
                        livePredictionConfidence = stablePrediction?.confidence
                            ?: it.livePredictionConfidence,
                        livePredictionTimeMs = stablePrediction?.let {
                            analysisResult.predictionTimeMs
                        } ?: it.livePredictionTimeMs
                    )
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Realtime analysis failed", exception)
                livePredictionVotes.clear()
                _uiState.update {
                    it.copy(
                        livePredictionLabel = "Gagal Menganalisis",
                        livePredictionConfidence = 0f,
                        livePredictionTimeMs = null,
                        error = exception.localizedMessage ?: "Gagal menganalisis kamera secara real-time"
                    )
                }
            } finally {
                isAnalyzingFrame = false
            }
        }
    }



    fun setFacingFront(isFront: Boolean) {
        _uiState.update {
            it.copy(
                isFacingFront = isFront,
                isFlashEnabled = if (isFront) false else it.isFlashEnabled
            )
        }
    }

    fun onCaptureStarted() {
        _uiState.update { it.copy(isCapturing = true) }
    }

    fun onCaptureFinished() {
        _uiState.update { it.copy(isCapturing = false) }
    }

    fun setError(error: String?) {
        _uiState.update { it.copy(error = error) }
    }

    fun latestAnalyzedFrameCopy(): Bitmap? {
        return latestAnalyzedBitmap?.copy(Bitmap.Config.ARGB_8888, false)
    }

    private fun updateStableRealtimePrediction(
        analysisResult: LiveAnalysisResult
    ): StableRealtimePrediction? {
        val label = analysisResult.bestLabel?.takeIf { it.isNotBlank() } ?: return null
        livePredictionVotes.addLast(
            LivePredictionVote(
                label = label,
                confidence = analysisResult.bestConfidence
            )
        )

        while (livePredictionVotes.size > REALTIME_VOTE_WINDOW_SIZE) {
            livePredictionVotes.removeFirst()
        }

        if (livePredictionVotes.size < MIN_REALTIME_VOTES_BEFORE_DISPLAY) {
            return null
        }

        return livePredictionVotes
            .groupBy { it.label }
            .map { (candidateLabel, votes) ->
                StableRealtimePrediction(
                    label = candidateLabel,
                    voteCount = votes.size,
                    confidence = votes.map { it.confidence }.average().toFloat()
                )
            }
            .maxWithOrNull(
                compareBy<StableRealtimePrediction> { it.voteCount }
                    .thenBy { it.confidence }
            )
    }
}

private data class LiveAnalysisResult(
    val frameWidth: Int,
    val frameHeight: Int,
    val bestLabel: String?,
    val bestConfidence: Float,
    val predictionTimeMs: Long
)

private data class LivePredictionVote(
    val label: String,
    val confidence: Float
)

private data class StableRealtimePrediction(
    val label: String,
    val voteCount: Int,
    val confidence: Float
)

data class CameraUiState(
    val isFlashEnabled: Boolean = false,
    val isFacingFront: Boolean = false,
    val isCapturing: Boolean = false,
    val error: String? = null,
    val livePredictionLabel: String = DEFAULT_PREDICTION_LABEL,
    val livePredictionConfidence: Float = 0f,
    val livePredictionTimeMs: Long? = null,
    val analysisFrameWidth: Int = 0,
    val analysisFrameHeight: Int = 0,
    val roiSizeFraction: Float = 0.8f,
    val roiVerticalBias: Float = -0.14f
)
