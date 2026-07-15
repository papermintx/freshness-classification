package com.skripsi.core.data.benchmark

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.skripsi.core.domain.model.CameraRoiBenchmarkSummary
import com.skripsi.core.domain.model.CameraRoiInferenceResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

class CameraRoiBenchmarkReportExporter @Inject constructor(
    private val context: Context
) {
    suspend fun export(
        results: List<CameraRoiInferenceResult>,
        summary: CameraRoiBenchmarkSummary
    ): Pair<File, File> = withContext(Dispatchers.IO) {
        val timestamp = LocalDateTime.now().format(FILE_TIMESTAMP_FORMAT)
        val csvFileName = "camera_roi_inference_report_$timestamp.csv"
        val summaryFileName = "camera_roi_inference_summary_$timestamp.json"
        val csvContent = buildCsv(results)
        val summaryContent = buildSummaryJson(summary).toString(2)
        val reportSubDir = buildReportSubDir(summary)

        val outputDir = resolveWritableReportDir(reportSubDir)
        val csvFile = File(outputDir, csvFileName)
        val summaryFile = File(outputDir, summaryFileName)

        csvFile.writeText(csvContent)
        summaryFile.writeText(summaryContent)

        copyToDownloads(csvFileName, "text/csv", csvContent, reportSubDir)
        copyToDownloads(summaryFileName, "application/json", summaryContent, reportSubDir)

        csvFile to summaryFile
    }

    private fun resolveWritableReportDir(reportSubDir: String): File {
        val candidates = listOfNotNull(
            context.getExternalFilesDir("$REPORT_DIR/$reportSubDir"),
            File(context.filesDir, "$REPORT_DIR/$reportSubDir")
        )

        return candidates.firstOrNull { directory ->
            runCatching {
                directory.mkdirs()
                directory.exists() && directory.isDirectory && directory.canWrite()
            }.getOrDefault(false)
        } ?: error("Tidak ada folder report yang bisa ditulis.")
    }

    private fun buildCsv(results: List<CameraRoiInferenceResult>): String {
        return buildString {
            appendLine(CSV_HEADER)
            results.forEach { result ->
                appendLine(
                    listOf(
                        result.timestamp,
                        result.sessionId,
                        result.deviceName,
                        result.androidVersion,
                        result.modelName,
                        result.modelFormat,
                        result.captureIndex.toString(),
                        result.distanceCm?.toString().orEmpty(),
                        result.sourceImageWidth.toString(),
                        result.sourceImageHeight.toString(),
                        result.roiImageWidth.toString(),
                        result.roiImageHeight.toString(),
                        result.modelInputWidth.toString(),
                        result.modelInputHeight.toString(),
                        result.intervalSincePreviousInferenceMs.toString(),
                        result.cameraLensFacing,
                        result.cameraFpsRange,
                        result.cameraMaxFps.toString(),
                        result.cameraMegapixels.toString(),
                        result.cameraResolution,
                        result.actualLabel,
                        result.predictedLabel,
                        result.confidence.toString(),
                        result.isCorrect.toString(),
                        result.roiCropTimeMs.toString(),
                        result.preprocessingTimeMs.toString(),
                        result.inferenceTimeMs.toString(),
                        result.totalTimeMs.toString(),
                        result.savedRoiImagePath.orEmpty()
                    ).joinToString(",") { it.csvEscape() }
                )
            }
        }
    }

    private fun buildSummaryJson(summary: CameraRoiBenchmarkSummary): JSONObject {
        return JSONObject()
            .put("sessionId", summary.sessionId)
            .put("actualLabel", summary.actualLabel)
            .put("distanceCm", summary.distanceCm ?: JSONObject.NULL)
            .put("cameraLensFacing", summary.cameraLensFacing)
            .put("cameraFpsRange", summary.cameraFpsRange)
            .put("cameraMaxFps", summary.cameraMaxFps)
            .put("cameraMegapixels", summary.cameraMegapixels)
            .put("cameraResolution", summary.cameraResolution)
            .put("totalCaptures", summary.totalCaptures)
            .put("correctPrediction", summary.correctPrediction)
            .put("wrongPrediction", summary.wrongPrediction)
            .put("accuracy", summary.accuracy)
            .put("meanInferenceMs", summary.meanInferenceMs)
            .put("medianInferenceMs", summary.medianInferenceMs)
            .put("minInferenceMs", summary.minInferenceMs)
            .put("maxInferenceMs", summary.maxInferenceMs)
            .put("p95InferenceMs", summary.p95InferenceMs)
            .put("meanPreprocessingMs", summary.meanPreprocessingMs)
            .put("meanRoiCropMs", summary.meanRoiCropMs)
            .put("meanIntervalSincePreviousInferenceMs", summary.meanIntervalSincePreviousInferenceMs)
            .put("meanTotalMs", summary.meanTotalMs)
    }

    private fun String.csvEscape(): String {
        val escaped = replace("\"", "\"\"")
        return if (contains(",") || contains("\"") || contains("\n")) "\"$escaped\"" else escaped
    }

    private fun buildReportSubDir(summary: CameraRoiBenchmarkSummary): String {
        val distanceFolder = summary.distanceCm
            ?.let { "$it cm" }
            ?: "tanpa jarak"
        val labelFolder = actualLabelDisplayName(summary.actualLabel).sanitizePathSegment()
        return "$distanceFolder/$labelFolder"
    }

    private fun actualLabelDisplayName(label: String): String {
        return when (label) {
            "fruits_fresh_banana" -> "pisang segar"
            "fruits_fresh_mango" -> "mangga segar"
            "fruits_fresh_orange" -> "jeruk segar"
            "fruits_rotten_banana" -> "pisang busuk"
            "fruits_rotten_mango" -> "mangga busuk"
            "fruits_rotten_orange" -> "jeruk busuk"
            "vegetables_fresh_carrot" -> "wortel segar"
            "vegetables_fresh_cucumber" -> "timun segar"
            "vegetables_fresh_tomato" -> "tomat segar"
            "vegetables_rotten_carrot" -> "wortel busuk"
            "vegetables_rotten_cucumber" -> "timun busuk"
            "vegetables_rotten_tomato" -> "tomat busuk"
            else -> label
        }.lowercase(Locale.forLanguageTag("id-ID"))
    }

    private fun String.sanitizePathSegment(): String {
        return replace(Regex("""[\\/:*?"<>|]"""), "_")
            .trim()
            .ifBlank { "tidak diketahui" }
    }

    private fun copyToDownloads(
        fileName: String,
        mimeType: String,
        content: String,
        reportSubDir: String
    ) {
        runCatching {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOWNLOADS}/CnnFreshScanResearch/$reportSubDir"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return@runCatching
            resolver.openOutputStream(uri)?.use { output ->
                output.write(content.toByteArray())
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
    }

    companion object {
        const val REPORT_DIR = "research_report"
        const val CSV_HEADER = "timestamp,sessionId,deviceName,androidVersion,modelName,modelFormat,captureIndex,distanceCm,sourceImageWidth,sourceImageHeight,roiImageWidth,roiImageHeight,modelInputWidth,modelInputHeight,intervalSincePreviousInferenceMs,cameraLensFacing,cameraFpsRange,cameraMaxFps,cameraMegapixels,cameraResolution,actualLabel,predictedLabel,confidence,isCorrect,roiCropTimeMs,preprocessingTimeMs,inferenceTimeMs,totalTimeMs,savedRoiImagePath"
        private val FILE_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    }
}
