package com.skripsi.cnnfreshscan.presentation.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skripsi.core.domain.model.CameraRoiBenchmarkSummary
import com.skripsi.core.domain.model.CameraRoiInferenceResult
import com.skripsi.core.domain.usecase.ExportCameraBenchmarkReportUseCase
import com.skripsi.core.domain.usecase.RunCameraRoiBenchmarkUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.os.SystemClock
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class CameraBenchmarkViewModel @Inject constructor(
    private val runCameraRoiBenchmarkUseCase: RunCameraRoiBenchmarkUseCase,
    private val exportCameraBenchmarkReportUseCase: ExportCameraBenchmarkReportUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraBenchmarkUiState())
    val uiState: StateFlow<CameraBenchmarkUiState> = _uiState.asStateFlow()
    private var lastBenchmarkInferenceStartedAtNanos: Long? = null

    fun setActualLabel(label: String) {
        _uiState.update { it.copy(actualLabel = label) }
    }

    fun setTargetCaptures(total: Int) {
        _uiState.update { it.copy(targetCaptures = total) }
    }

    fun setDistanceCm(distanceCm: Int?) {
        _uiState.update { it.copy(distanceCm = distanceCm) }
    }

    fun startBatchTest() {
        val state = _uiState.value
        if (state.status == CameraBenchmarkStatus.Running || state.status == CameraBenchmarkStatus.Countdown) return

        val sessionId = "camera_roi_${SESSION_FORMATTER.format(LocalDateTime.now())}"
        lastBenchmarkInferenceStartedAtNanos = null
        _uiState.update {
            it.copy(
                status = CameraBenchmarkStatus.Countdown,
                sessionId = sessionId,
                currentCaptureIndex = 0,
                results = emptyList(),
                summary = null,
                warmUpCompleted = false,
                countdownValue = COUNTDOWN_SECONDS,
                message = null
            )
        }

        viewModelScope.launch {
            for (count in COUNTDOWN_SECONDS downTo 1) {
                _uiState.update {
                    it.copy(
                        countdownValue = count,
                        message = null
                    )
                }
                delay(1000L)
            }

            _uiState.update {
                it.copy(
                    status = CameraBenchmarkStatus.Running,
                    countdownValue = 0,
                    message = "Pemanasan inferensi..."
                )
            }

            runCatching {
                runCameraRoiBenchmarkUseCase.warmUp()
            }.onSuccess {
                _uiState.update { it.copy(warmUpCompleted = true) }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        status = CameraBenchmarkStatus.Error,
                        warmUpCompleted = false,
                        countdownValue = 0,
                        message = throwable.localizedMessage ?: "Pemanasan inferensi gagal."
                    )
                }
            }
        }
    }

    fun stopBatchTest() {
        val state = _uiState.value
        if (state.status != CameraBenchmarkStatus.Running && state.status != CameraBenchmarkStatus.Countdown) return
        _uiState.update {
            it.copy(
                status = CameraBenchmarkStatus.Completed,
                countdownValue = 0,
                message = "Pengujian dihentikan."
            )
        }
        lastBenchmarkInferenceStartedAtNanos = null
    }



    fun shouldContinueCapture(): Boolean {
        val state = _uiState.value
        return state.status == CameraBenchmarkStatus.Running &&
            state.warmUpCompleted &&
            state.currentCaptureIndex < state.targetCaptures
    }

    suspend fun processCapturedBitmap(
        captureIndex: Int,
        bitmap: Bitmap,
        cameraLensFacing: String,
        cameraFpsRange: String,
        cameraMaxFps: Int,
        cameraMegapixels: Double,
        cameraResolution: String
    ) {
        val state = _uiState.value
        if (state.status != CameraBenchmarkStatus.Running || !state.warmUpCompleted) return
        val inferenceStartedAtNanos = SystemClock.elapsedRealtimeNanos()
        val intervalSincePreviousInferenceMs = lastBenchmarkInferenceStartedAtNanos
            ?.let { previousStart -> (inferenceStartedAtNanos - previousStart) / 1_000_000.0 }
            ?: 0.0
        lastBenchmarkInferenceStartedAtNanos = inferenceStartedAtNanos

        runCatching {
            runCameraRoiBenchmarkUseCase.processCameraFrame(
                sessionId = state.sessionId,
                captureIndex = captureIndex,
                actualLabel = state.actualLabel,
                distanceCm = state.distanceCm,
                intervalSincePreviousInferenceMs = intervalSincePreviousInferenceMs,
                cameraLensFacing = cameraLensFacing,
                cameraFpsRange = cameraFpsRange,
                cameraMaxFps = cameraMaxFps,
                cameraMegapixels = cameraMegapixels,
                cameraResolution = cameraResolution,
                bitmap = bitmap,
                saveRoiImage = true
            )
        }.onSuccess { result ->
            val updatedResults = _uiState.value.results + result
            val summary = runCameraRoiBenchmarkUseCase.summarize(
                sessionId = state.sessionId,
                actualLabel = state.actualLabel,
                distanceCm = state.distanceCm,
                results = updatedResults
            )
            _uiState.update {
                it.copy(
                    currentCaptureIndex = captureIndex,
                    results = updatedResults,
                    summary = summary,
                    message = "Pengambilan gambar $captureIndex/${it.targetCaptures} selesai."
                )
            }

            if (captureIndex >= _uiState.value.targetCaptures) {
                _uiState.update {
                    it.copy(
                        status = CameraBenchmarkStatus.Completed,
                        countdownValue = 0,
                        message = "Pengujian selesai."
                    )
                }
            }
        }.onFailure { throwable ->
            _uiState.update {
                it.copy(
                    status = CameraBenchmarkStatus.Error,
                    message = throwable.localizedMessage ?: "Gagal memproses gambar ROI."
                )
            }
        }
    }

    fun exportReport() {
        viewModelScope.launch {
            val state = _uiState.value
            val summary = state.summary
            if (state.results.isEmpty() || summary == null) {
                _uiState.update { it.copy(message = "Belum ada hasil pengujian ROI kamera untuk diekspor.") }
                return@launch
            }

            runCatching {
                exportCameraBenchmarkReportUseCase(state.results, summary)
            }.onSuccess { (csvFile, summaryFile) ->
                _uiState.update {
                    it.copy(
                        lastCsvFile = csvFile,
                        lastSummaryFile = summaryFile,
                        message = "Laporan tersimpan di Downloads/CnnFreshScanResearch berdasarkan jarak dan label aktual."
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(message = throwable.localizedMessage ?: "Gagal mengekspor laporan ROI kamera.")
                }
            }
        }
    }

    fun clear() {
        lastBenchmarkInferenceStartedAtNanos = null
        _uiState.value = CameraBenchmarkUiState(message = "Hasil pengujian ROI kamera dibersihkan.")
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    companion object {
        const val COUNTDOWN_SECONDS = 3
        val LABELS = listOf(
            "fruits_fresh_banana",
            "fruits_fresh_mango",
            "fruits_fresh_orange",
            "fruits_rotten_banana",
            "fruits_rotten_mango",
            "fruits_rotten_orange",
            "vegetables_fresh_carrot",
            "vegetables_fresh_cucumber",
            "vegetables_fresh_tomato",
            "vegetables_rotten_carrot",
            "vegetables_rotten_cucumber",
            "vegetables_rotten_tomato"
        )
        val LABEL_DISPLAY_NAMES = mapOf(
            "fruits_fresh_banana" to "Pisang Segar",
            "fruits_fresh_mango" to "Mangga Segar",
            "fruits_fresh_orange" to "Jeruk Segar",
            "fruits_rotten_banana" to "Pisang Busuk",
            "fruits_rotten_mango" to "Mangga Busuk",
            "fruits_rotten_orange" to "Jeruk Busuk",
            "vegetables_fresh_carrot" to "Wortel Segar",
            "vegetables_fresh_cucumber" to "Timun Segar",
            "vegetables_fresh_tomato" to "Tomat Segar",
            "vegetables_rotten_carrot" to "Wortel Busuk",
            "vegetables_rotten_cucumber" to "Timun Busuk",
            "vegetables_rotten_tomato" to "Tomat Busuk"
        )
        val CAPTURE_OPTIONS = listOf(10, 30, 50, 100)
        val DISTANCE_OPTIONS_CM = listOf(10, 20, 30)
        private val SESSION_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

        fun displayNameForLabel(label: String): String {
            return LABEL_DISPLAY_NAMES[label] ?: label
        }
    }
}

enum class CameraBenchmarkStatus {
    Idle,
    Countdown,
    Running,
    Completed,
    Error
}

data class CameraBenchmarkUiState(
    val status: CameraBenchmarkStatus = CameraBenchmarkStatus.Idle,
    val sessionId: String = "",
    val actualLabel: String = CameraBenchmarkViewModel.LABELS.first(),
    val distanceCm: Int? = null,
    val targetCaptures: Int = 10,
    val currentCaptureIndex: Int = 0,
    val countdownValue: Int = 0,
    val warmUpCompleted: Boolean = false,
    val results: List<CameraRoiInferenceResult> = emptyList(),
    val summary: CameraRoiBenchmarkSummary? = null,
    val lastCsvFile: File? = null,
    val lastSummaryFile: File? = null,
    val message: String? = null
)
