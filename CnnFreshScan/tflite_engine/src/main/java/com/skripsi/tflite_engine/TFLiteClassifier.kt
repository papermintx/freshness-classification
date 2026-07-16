package com.skripsi.tflite_engine

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.tflite.gpu.support.TfLiteGpu
import com.google.android.gms.tflite.java.TfLite
import com.google.android.gms.tflite.client.TfLiteInitializationOptions
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
import org.tensorflow.lite.gpu.GpuDelegateFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TFLiteClassifier(private val context: Context) : ProduceClassifier {

    override val modelName: String = "mobilenetv2_float16.tflite"
    override val modelFormat: String = "TensorFlow Lite Float16"
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
     * Menggunakan GPU Delegate untuk inferensi Float16 yang lebih cepat.
     * Jika GPU tidak didukung, otomatis fallback ke CPU.
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
                // Langkah 1: Cek apakah GPU Delegate tersedia di perangkat ini
                val isGpuAvailable = try {
                    TfLiteGpu.isGpuDelegateAvailable(context).await()
                } catch (e: Exception) {
                    Log.w("TFLiteClassifier", "⚠️ Gagal cek GPU availability: ${e.message}")
                    false
                }
                Log.d("TFLiteClassifier", "GPU available: $isGpuAvailable")

                // Langkah 2: Initialize TFLite runtime dengan GPU support jika tersedia.
                // Menggunakan TfLiteInitializationOptions dari com.google.android.gms.tflite.client
                val initOptions = TfLiteInitializationOptions.builder()
                    .setEnableGpuDelegateSupport(isGpuAvailable)
                    .build()
                TfLite.initialize(context, initOptions).await()

                // Langkah 3: Load file .tflite dari folder assets
                val modelFile = FileUtil.loadMappedFile(context, modelName)

                // Langkah 4: Konfigurasi InterpreterApi Options
                val options = InterpreterApi.Options().apply {
                    setRuntime(InterpreterApi.Options.TfLiteRuntime.FROM_SYSTEM_ONLY)
                    if (isGpuAvailable) {
                        // Gunakan GpuDelegateFactory agar tidak melanggar batasan Play Services TFLite
                        addDelegateFactory(GpuDelegateFactory())
                        Log.d("TFLiteClassifier", "🚀 GPU Delegate aktif! Float16 inferensi di GPU")
                    } else {
                        numThreads = 4
                        Log.w("TFLiteClassifier", "⚠️ GPU tidak tersedia, fallback ke CPU (4 threads)")
                    }
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
                Log.d("TFLiteClassifier", "✅ Mesin AI Siap! Model: $modelName | Total kelas: ${labels.size}")
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

            // 1. Load Bitmap ke TensorImage FLOAT32
            var tensorImage = TensorImage(DataType.FLOAT32)
            tensorImage.load(bitmap)

            // 2. Resize gambar otomatis jadi 224x224 & Normalize ke [-1, 1]
            // Float16 model menggunakan range yang sama dengan Float32 MobileNetV2
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
     * Melakukan inference pada TensorImage yang sudah di-preprocess.
     * Float16 model: input dan output bertipe FLOAT32 di sisi Java/Kotlin,
     * konversi ke Float16 dilakukan otomatis oleh GPU Delegate secara internal.
     * Method ini dipanggil oleh repository layer setelah preprocessing.
     */
    override fun classifyFromTensor(tensorImage: TensorImage): List<TFLiteResult> {
        if (!isInitialized || interpreter == null) {
            Log.e("TFLiteClassifier", "❌ Classifier not initialized. Call initialize() first.")
            return emptyList()
        }

        try {
            Log.d("TFLiteClassifier", "🔍 Running Float16 inference on GPU...")

            // Input: langsung gunakan FLOAT32 buffer dari TensorImage
            // GPU Delegate menangani konversi Float32 → Float16 secara internal
            val inputBuffer: ByteBuffer = tensorImage.buffer

            // Output: alokasikan buffer FLOAT32 (4 bytes per float)
            val outputBufferSize = labels.size * 4 // 4 bytes per FLOAT32
            val outputBuffer = ByteBuffer.allocateDirect(outputBufferSize)
            outputBuffer.order(ByteOrder.nativeOrder())

            // Run inference
            interpreter?.run(inputBuffer, outputBuffer)
            Log.d("TFLiteClassifier", "✓ Inference completed")

            // Baca output langsung sebagai FloatArray (tidak perlu dequantize)
            val floatValues = FloatArray(labels.size)
            outputBuffer.rewind()
            outputBuffer.asFloatBuffer().get(floatValues)

            // Bungkus ke TensorBuffer FLOAT32 untuk mapping label
            val outputTensorBuffer = TensorBuffer.createFixedSize(intArrayOf(1, labels.size), DataType.FLOAT32)
            outputTensorBuffer.loadArray(floatValues)

            // Map probabilities ke label
            val tensorLabel = TensorLabel(labels, outputTensorBuffer)
            val floatMap = tensorLabel.mapWithFloatValue

            // Sort by confidence descending dan map ke TFLiteResult
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
