package com.skripsi.core.data.benchmark

import com.skripsi.core.domain.model.CameraRoiBenchmarkSummary
import com.skripsi.core.domain.model.CameraRoiInferenceResult
import kotlin.math.ceil

object BenchmarkSummaryCalculator {
    fun summarizeCameraRoi(
        sessionId: String,
        actualLabel: String,
        distanceCm: Int?,
        results: List<CameraRoiInferenceResult>
    ): CameraRoiBenchmarkSummary {
        val inferenceTimes = results.map { it.inferenceTimeMs }.sorted()
        val correct = results.count { it.isCorrect }
        val wrong = results.size - correct

        return CameraRoiBenchmarkSummary(
            sessionId = sessionId,
            actualLabel = actualLabel,
            distanceCm = distanceCm,
            cameraLensFacing = results.firstOrNull()?.cameraLensFacing.orEmpty(),
            cameraFpsRange = results.firstOrNull()?.cameraFpsRange.orEmpty(),
            cameraMaxFps = results.firstOrNull()?.cameraMaxFps ?: 0,
            cameraMegapixels = results.firstOrNull()?.cameraMegapixels ?: 0.0,
            cameraResolution = results.firstOrNull()?.cameraResolution.orEmpty(),
            totalCaptures = results.size,
            correctPrediction = correct,
            wrongPrediction = wrong,
            accuracy = if (results.isNotEmpty()) correct.toDouble() / results.size else 0.0,
            meanInferenceMs = results.map { it.inferenceTimeMs }.averageOrZero(),
            medianInferenceMs = percentile(inferenceTimes, 50.0),
            minInferenceMs = inferenceTimes.firstOrNull() ?: 0.0,
            maxInferenceMs = inferenceTimes.lastOrNull() ?: 0.0,
            p95InferenceMs = percentile(inferenceTimes, 95.0),
            meanPreprocessingMs = results.map { it.preprocessingTimeMs }.averageOrZero(),
            meanRoiCropMs = results.map { it.roiCropTimeMs }.averageOrZero(),
            meanIntervalSincePreviousInferenceMs = results
                .map { it.intervalSincePreviousInferenceMs }
                .filter { it > 0.0 }
                .averageOrZero(),
            meanTotalMs = results.map { it.totalTimeMs }.averageOrZero()
        )
    }

    fun percentile(sortedValues: List<Double>, percentile: Double): Double {
        if (sortedValues.isEmpty()) return 0.0
        val rank = ceil((percentile / 100.0) * sortedValues.size).toInt()
        return sortedValues[(rank - 1).coerceIn(0, sortedValues.lastIndex)]
    }

    private fun List<Double>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()
}
