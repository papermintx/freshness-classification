package com.skripsi.tflite_engine

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.tflite.java.TfLite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

class TFLiteClassifier(private val context: Context) : ProduceClassifier {

    override val modelName: String = "mobilenetv2_int8.tflite"
    override val modelFormat: String = "TensorFlow Lite INT8"
    override val inputWidth: Int = 224
    override val inputHeight: Int = 224

    private var interpreter: InterpreterApi? = null
    private var labels: List<String> = emptyList()
    private var isInitialized = false

    // Ukuran standar dari MobileNetV2
    private val imageSizeX = inputWidth
    private val imageSizeY = inputHeight

    /**
     * Initializes the TFLite interpreter using Google Play Services.
     * This method must be called before classify().
     * returns true if initialization is successful, false otherwise.
     */
    override suspend fun initialize(): Boolean {
        if (isInitialized && interpreter != null) {
            Log.d("TFLiteClassifier", "Classifier already initialized")
            return true
        }
        return withContext(Dispatchers.IO) {
            try {
                // Initialize TFLite runtime from Google Play Services
                TfLite.initialize(context).await()

                // Load file .tflite dari folder assets
                val modelFile = FileUtil.loadMappedFile(context, modelName)

                // Setting options
                val options = InterpreterApi.Options().apply {
                    setRuntime(InterpreterApi.Options.TfLiteRuntime.FROM_SYSTEM_ONLY)
                    numThreads = 4
                }

                // Create the InterpreterApi instance
                interpreter = InterpreterApi.create(modelFile, options)

                // Load daftar label kelas dari labels.txt
                Log.d("TFLiteClassifier", "Loading labels from labels.txt")
                labels = FileUtil.loadLabels(context, "labels.txt")
                Log.d("TFLiteClassifier", "✓ Labels loaded: ${labels.size} classes")
                
                if (labels.isEmpty()) {
                    throw IllegalStateException("Labels file is empty!")
                }
                
                Log.d("TFLiteClassifier", "🎯 Class labels: $labels")

                isInitialized = true
                Log.d("TFLiteClassifier", "✅ Mesin AI Siap! Total kelas: ${labels.size}")
                true
            } catch (e: Exception) {
                Log.e("TFLiteClassifier", "❌ Gagal initialize model", e)
                e.printStackTrace()
                isInitialized = false
                interpreter = null
                false
            }
        }
    }

    override fun classify(bitmap: Bitmap): List<TFLiteResult> {
        // Backward compatibility: preprocess dalam method ini jika dipanggil langsung
        if (!isInitialized || interpreter == null) {
            Log.e("TFLiteClassifier", "❌ Classifier not initialized. Call initialize() first.")
            return emptyList()
        }

        try {
            Log.d("TFLiteClassifier", "Processing bitmap: ${bitmap.width}x${bitmap.height}")
            
            // 1. Ubah Bitmap jadi format FLOAT32 (selalu FLOAT32 untuk TensorImage agar tidak crash)
            var tensorImage = TensorImage(DataType.FLOAT32)
            tensorImage.load(bitmap)

            // 2. Resize gambar otomatis jadi 224x224 & Normalize
            val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(imageSizeY, imageSizeX, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(127.5f, 127.5f)) // MobileNetV2: normalize pixel [0,255] ke [-1,1]
                .build()
            tensorImage = imageProcessor.process(tensorImage)
            Log.d("TFLiteClassifier", "✓ Image preprocessed: resized to 224x224, normalized [-1,1]")

            return classifyFromTensor(tensorImage)
        } catch (e: Exception) {
            Log.e("TFLiteClassifier", "❌ Error during classification", e)
            e.printStackTrace()
            return emptyList()
        }
    }

    override fun getInputDataType(): DataType {
        if (!isInitialized || interpreter == null) {
            return DataType.FLOAT32
        }
        return interpreter?.getInputTensor(0)?.dataType() ?: DataType.FLOAT32
    }

    override fun getLabels(): List<String> = labels

    override fun getLabelCount(): Int = labels.size

    /**
     * Melakukan inference pada TensorImage yang sudah di-preprocess
     * Method ini dipanggil oleh repository layer setelah preprocessing
     */
    override fun classifyFromTensor(tensorImage: TensorImage): List<TFLiteResult> {
        if (!isInitialized || interpreter == null) {
            Log.e("TFLiteClassifier", "❌ Classifier not initialized. Call initialize() first.")
            return emptyList()
        }

        try {
            Log.d("TFLiteClassifier", "🔍 Running inference on preprocessed tensor...")
            
            // Get input tensor details
            val inputTensor = interpreter?.getInputTensor(0) ?: throw IllegalStateException("Input tensor not found")
            val inputDataType = inputTensor.dataType()
            
            // Convert to byte buffer if model expects INT8 or UINT8 input
            val inputBuffer = if (inputDataType == DataType.INT8 || inputDataType == DataType.UINT8) {
                val quantizationParams = inputTensor.quantizationParams()
                val scale = quantizationParams.scale
                val zeroPoint = quantizationParams.zeroPoint

                val floatValues = tensorImage.tensorBuffer.floatArray
                val byteBuffer = ByteBuffer.allocateDirect(floatValues.size)
                byteBuffer.order(ByteOrder.nativeOrder())

                if (inputDataType == DataType.INT8) {
                    for (value in floatValues) {
                        val quantValue =
                            ((value / scale) + zeroPoint)
                                .roundToInt()
                                .coerceIn(-128, 127)
                                .toByte()
                        byteBuffer.put(quantValue)
                    }
                } else {
                    for (value in floatValues) {
                        val quantValue = ((value / scale) + zeroPoint).roundToInt().coerceIn(0, 255).toByte()
                        byteBuffer.put(quantValue)
                    }
                }
                byteBuffer.rewind()
                byteBuffer
            } else {
                tensorImage.buffer
            }
            
            // Get output tensor details
            val outputTensor = interpreter?.getOutputTensor(0) ?: throw IllegalStateException("Output tensor not found")
            val outputDataType = outputTensor.dataType()
            
            // Allocate a raw direct ByteBuffer for output to prevent TensorBuffer type errors
            val elementSize = if (outputDataType == DataType.FLOAT32) 4 else 1
            val outputBufferSize = labels.size * elementSize
            val outputBuffer = ByteBuffer.allocateDirect(outputBufferSize)
            outputBuffer.order(ByteOrder.nativeOrder())

            // Run inference
            interpreter?.run(inputBuffer, outputBuffer)
            Log.d("TFLiteClassifier", "✓ Inference completed")

            // Dequantize the output manually if needed, and prepare a float array
            val floatValues = when (outputDataType) {
                DataType.INT8 -> {
                    val scale = outputTensor.quantizationParams().scale
                    val zeroPoint = outputTensor.quantizationParams().zeroPoint
                    val quantizedOutput = ByteArray(labels.size)
                    outputBuffer.rewind()
                    outputBuffer.get(quantizedOutput)
                    FloatArray(quantizedOutput.size) { i ->
                        (quantizedOutput[i].toFloat() - zeroPoint) * scale
                    }
                }
                DataType.UINT8 -> {
                    val scale = outputTensor.quantizationParams().scale
                    val zeroPoint = outputTensor.quantizationParams().zeroPoint
                    val quantizedOutput = ByteArray(labels.size)
                    outputBuffer.rewind()
                    outputBuffer.get(quantizedOutput)
                    FloatArray(quantizedOutput.size) { i ->
                        ((quantizedOutput[i].toInt() and 0xFF).toFloat() - zeroPoint) * scale
                    }
                }
                else -> {
                    val tempFloats = FloatArray(labels.size)
                    outputBuffer.rewind()
                    outputBuffer.asFloatBuffer().get(tempFloats)
                    tempFloats
                }
            }

            // Create a float-based TensorBuffer for TensorLabel mapping (FLOAT32 is fully supported by TensorBuffer)
            val dequantizedBuffer = TensorBuffer.createFixedSize(intArrayOf(1, labels.size), DataType.FLOAT32)
            dequantizedBuffer.loadArray(floatValues)

            // Map probabilities to labels
            val tensorLabel = TensorLabel(labels, dequantizedBuffer)
            val floatMap = tensorLabel.mapWithFloatValue

            // Sort by confidence descending and map to TFLiteResult
            val results = floatMap.entries
                .sortedByDescending { it.value }
                .map {
                    TFLiteResult(label = it.key, confidence = it.value)
                }
            
            // Log top 3 results
            Log.d("TFLiteClassifier", "📊 Top predictions:")
            results.take(3).forEachIndexed { index, result ->
                Log.d("TFLiteClassifier", "  ${index + 1}. ${result.label}: ${(result.confidence * 100).toInt()}%")
            }
            
            return results

        } catch (e: Exception) {
            Log.e("TFLiteClassifier", "❌ Error during classification", e)
            e.printStackTrace()
            return emptyList()
        }
    }

    // Wajib dipanggil nanti pas nutup kamera biar RAM nggak bocor
    override fun close() {
        interpreter?.close()
        interpreter = null
        isInitialized = false
    }
}
