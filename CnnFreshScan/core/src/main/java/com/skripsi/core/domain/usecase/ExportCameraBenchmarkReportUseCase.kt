package com.skripsi.core.domain.usecase

import com.skripsi.core.data.benchmark.CameraRoiBenchmarkReportExporter
import com.skripsi.core.domain.model.CameraRoiBenchmarkSummary
import com.skripsi.core.domain.model.CameraRoiInferenceResult
import java.io.File

class ExportCameraBenchmarkReportUseCase(
    private val exporter: CameraRoiBenchmarkReportExporter
) {
    suspend operator fun invoke(
        results: List<CameraRoiInferenceResult>,
        summary: CameraRoiBenchmarkSummary
    ): Pair<File, File> {
        return exporter.export(results, summary)
    }
}
