package com.skripsi.tflite_engine

import android.graphics.Bitmap
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage

interface ProduceClassifier {
    val modelName: String
    val modelFormat: String
    val inputWidth: Int
    val inputHeight: Int

    suspend fun initialize(): Boolean
    fun classify(bitmap: Bitmap): List<TFLiteResult>
    fun classifyFromTensor(tensorImage: TensorImage): List<TFLiteResult>
    fun getInputDataType(): DataType
    fun getLabels(): List<String>
    fun getLabelCount(): Int
    fun close()
}
