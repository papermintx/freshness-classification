package com.skripsi.core.data.benchmark

import com.skripsi.core.domain.model.CameraRoiInferenceResult
import org.junit.Assert.assertEquals
import org.junit.Test

class BenchmarkSummaryCalculatorTest {
    @Test
    fun summarizeCameraRoiCalculatesTimingAndAccuracy() {
        val results = listOf(
            result(1, true, inferenceMs = 10.0),
            result(2, false, inferenceMs = 20.0),
            result(3, true, inferenceMs = 30.0),
            result(4, true, inferenceMs = 40.0)
        )

        val summary = BenchmarkSummaryCalculator.summarizeCameraRoi(
            sessionId = "session-test",
            actualLabel = "fruits_fresh_banana",
            distanceCm = 10,
            results = results
        )

        assertEquals(10, summary.distanceCm)
        assertEquals("Belakang", summary.cameraLensFacing)
        assertEquals("15-30|30-30", summary.cameraFpsRange)
        assertEquals(30, summary.cameraMaxFps)
        assertEquals(12.0, summary.cameraMegapixels, 0.0001)
        assertEquals("4000x3000", summary.cameraResolution)
        assertEquals(4, summary.totalCaptures)
        assertEquals(3, summary.correctPrediction)
        assertEquals(1, summary.wrongPrediction)
        assertEquals(0.75, summary.accuracy, 0.0001)
        assertEquals(25.0, summary.meanInferenceMs, 0.0001)
        assertEquals(20.0, summary.medianInferenceMs, 0.0001)
        assertEquals(10.0, summary.minInferenceMs, 0.0001)
        assertEquals(40.0, summary.maxInferenceMs, 0.0001)
        assertEquals(40.0, summary.p95InferenceMs, 0.0001)
        assertEquals(70.0, summary.meanIntervalSincePreviousInferenceMs, 0.0001)
    }

    private fun result(index: Int, correct: Boolean, inferenceMs: Double) = CameraRoiInferenceResult(
        timestamp = "2026-05-18T00:00:00Z",
        sessionId = "session-test",
        deviceName = "test-device",
        androidVersion = "Android test",
        modelName = "mobilenetv2_float16.tflite",
        modelFormat = "TensorFlow Lite float16",
        captureIndex = index,
        distanceCm = 10,
        sourceImageWidth = 1080,
        sourceImageHeight = 1920,
        roiImageWidth = 864,
        roiImageHeight = 864,
        modelInputWidth = 224,
        modelInputHeight = 224,
        intervalSincePreviousInferenceMs = if (index == 1) 0.0 else 70.0,
        cameraLensFacing = "Belakang",
        cameraFpsRange = "15-30|30-30",
        cameraMaxFps = 30,
        cameraMegapixels = 12.0,
        cameraResolution = "4000x3000",
        actualLabel = "fruits_fresh_banana",
        predictedLabel = if (correct) "fruits_fresh_banana" else "fruits_rotten_banana",
        confidence = 0.9f,
        isCorrect = correct,
        roiCropTimeMs = 2.0,
        preprocessingTimeMs = 4.0,
        inferenceTimeMs = inferenceMs,
        totalTimeMs = inferenceMs + 6.0,
        savedRoiImagePath = null
    )
}
