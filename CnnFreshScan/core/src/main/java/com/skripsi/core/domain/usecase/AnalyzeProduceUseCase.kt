package com.skripsi.core.domain.usecase

import android.graphics.Bitmap
import com.skripsi.core.domain.model.ProduceClassificationResult
import com.skripsi.core.domain.repository.ProduceRepository

class AnalyzeProduceUseCase(
    private val produceRepository: ProduceRepository
) {
    suspend operator fun invoke(bitmap: Bitmap): List<ProduceClassificationResult> {
        return produceRepository.analyzeImage(bitmap)
    }
}
