package com.skripsi.core.data.benchmark

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.skripsi.core.data.image.ImagePreprocessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImagePreprocessorInstrumentedTest {
    @Test
    fun preprocessToFloatArrayResizesTo224RgbAndNormalizesToMinusOneUntilOne() {
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(255, 127, 0))
        }

        val tensor = ImagePreprocessor().preprocessToFloatArray(bitmap)

        assertEquals(224 * 224 * 3, tensor.size)
        assertTrue(tensor.all { it in -1.0f..1.0f })
        assertTrue(tensor[0] > 0.99f)
        assertTrue(tensor[2] < -0.99f)
    }
}
