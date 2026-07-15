package com.skripsi.core.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.skripsi.core.data.image.ImagePreprocessor
import com.skripsi.core.data.image.RoiCropper
import com.skripsi.core.domain.model.ProduceClassificationResult
import com.skripsi.core.domain.repository.ProduceRepository
import com.skripsi.tflite_engine.TFLiteClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ProduceRepositoryImpl @Inject constructor(
    private val tfliteClassifier: TFLiteClassifier,
    private val roiCropper: RoiCropper,
    private val imagePreprocessor: ImagePreprocessor
) : ProduceRepository {

    private val initializationLock = Mutex()

    @Volatile
    private var classifierReady: Boolean = false

    override suspend fun analyzeImage(bitmap: Bitmap): List<ProduceClassificationResult> {
        return withContext(Dispatchers.IO) {
            if (!classifierReady) {
                initializationLock.withLock {
                    if (!classifierReady) {
                        Log.d(TAG, "Initializing TFLite classifier...")
                        val initialized = tfliteClassifier.initialize()
                        if (!initialized) {
                            throw RuntimeException("Failed to initialize TFLite classifier")
                        }
                        classifierReady = true
                        Log.d(TAG, "TFLite classifier initialized successfully")
                    }
                }
            }

            val roiBitmap = roiCropper.cropCenterSquare(bitmap)
            val preprocessedTensorImage = imagePreprocessor.preprocess(roiBitmap)
            val tfliteResults = tfliteClassifier.classifyFromTensor(preprocessedTensorImage)

            if (tfliteResults.isEmpty()) {
                Log.w(TAG, "No results from classifier - returning empty list")
            }

            tfliteResults.map { result ->
                ProduceClassificationResult(
                    name = result.label,
                    accuracyScore = result.confidence
                )
            }
        }
    }

    private companion object {
        const val TAG = "ProduceRepository"
    }
}
