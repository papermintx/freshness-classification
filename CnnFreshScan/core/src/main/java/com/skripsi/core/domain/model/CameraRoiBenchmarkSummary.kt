package com.skripsi.core.domain.model

data class CameraRoiBenchmarkSummary(
    val sessionId: String,
    val actualLabel: String,
    val distanceCm: Int?,
    val cameraLensFacing: String,
    val cameraFpsRange: String,
    val cameraMaxFps: Int,
    val cameraMegapixels: Double,
    val cameraResolution: String,
    val totalCaptures: Int,
    val correctPrediction: Int,
    val wrongPrediction: Int,
    val accuracy: Double,
    val meanInferenceMs: Double,
    val medianInferenceMs: Double,
    val minInferenceMs: Double,
    val maxInferenceMs: Double,
    val p95InferenceMs: Double,
    val meanPreprocessingMs: Double,
    val meanRoiCropMs: Double,
    val meanIntervalSincePreviousInferenceMs: Double,
    val meanTotalMs: Double
)
