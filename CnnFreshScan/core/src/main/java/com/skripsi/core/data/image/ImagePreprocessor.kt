package com.skripsi.core.data.image

import android.graphics.Bitmap
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import javax.inject.Inject
import androidx.core.graphics.scale

class ImagePreprocessor @Inject constructor() {
    private val processor = ImageProcessor.Builder()
        .add(ResizeOp(
            INPUT_HEIGHT,
            INPUT_WIDTH,
            ResizeOp.ResizeMethod.BILINEAR)
        )
        .add(NormalizeOp(127.5f, 127.5f))
        .build()

    fun preprocess(bitmap: Bitmap): TensorImage {
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        return processor.process(tensorImage)
    }

    fun preprocessToFloatArray(bitmap: Bitmap): FloatArray {
        val scaledBitmap = bitmap.scale(INPUT_WIDTH, INPUT_HEIGHT)
        val pixels = IntArray(INPUT_WIDTH * INPUT_HEIGHT)
        scaledBitmap.getPixels(pixels, 0, INPUT_WIDTH, 0, 0, INPUT_WIDTH, INPUT_HEIGHT)

        val output = FloatArray(INPUT_WIDTH * INPUT_HEIGHT * RGB_CHANNELS)
        var outputIndex = 0
        pixels.forEach { pixel ->
            val red = (pixel shr 16) and 0xFF
            val green = (pixel shr 8) and 0xFF
            val blue = pixel and 0xFF
            output[outputIndex++] = normalizeMobileNetV2(red)
            output[outputIndex++] = normalizeMobileNetV2(green)
            output[outputIndex++] = normalizeMobileNetV2(blue)
        }
        return output
    }

    private fun normalizeMobileNetV2(value: Int): Float = (value - 127.5f) / 127.5f

    companion object {
        const val INPUT_WIDTH = 224
        const val INPUT_HEIGHT = 224
        const val RGB_CHANNELS = 3
    }
}
