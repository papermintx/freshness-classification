package com.skripsi.core.domain.repository

import android.graphics.Bitmap
import com.skripsi.core.domain.model.ProduceClassificationResult

interface ProduceRepository {
    suspend fun analyzeImage(bitmap: Bitmap): List<ProduceClassificationResult>
}
