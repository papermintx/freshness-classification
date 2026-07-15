package com.skripsi.core.domain.usecase

import android.graphics.Bitmap
import com.skripsi.core.data.benchmark.CameraRoiBenchmarkRunner
import com.skripsi.core.domain.model.CameraRoiBenchmarkSummary
import com.skripsi.core.domain.model.CameraRoiInferenceResult

class RunCameraRoiBenchmarkUseCase(
    private val runner: CameraRoiBenchmarkRunner
) {
    suspend fun warmUp(iterations: Int = CameraRoiBenchmarkRunner.DEFAULT_WARM_UP_ITERATIONS) {
        runner.warmUp(iterations)
    }

    suspend fun processCameraFrame(
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
    ): CameraRoiInferenceResult {
        return runner.runSingleCapture(
            sessionId = sessionId,
            captureIndex = captureIndex,
            actualLabel = actualLabel,
            distanceCm = distanceCm,
            intervalSincePreviousInferenceMs = intervalSincePreviousInferenceMs,
            cameraLensFacing = cameraLensFacing,
            cameraFpsRange = cameraFpsRange,
            cameraMaxFps = cameraMaxFps,
            cameraMegapixels = cameraMegapixels,
            cameraResolution = cameraResolution,
            bitmap = bitmap,
            saveRoiImage = saveRoiImage
        )
    }

    fun summarize(
        sessionId: String,
        actualLabel: String,
        distanceCm: Int?,
        results: List<CameraRoiInferenceResult>
    ): CameraRoiBenchmarkSummary {
        return runner.summarize(sessionId, actualLabel, distanceCm, results)
    }
}
