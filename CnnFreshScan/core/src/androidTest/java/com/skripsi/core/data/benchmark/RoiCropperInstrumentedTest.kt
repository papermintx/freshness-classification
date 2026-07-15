package com.skripsi.core.data.benchmark

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.skripsi.core.data.image.RoiCropper
import com.skripsi.core.data.image.RoiGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoiCropperInstrumentedTest {
    @Test
    fun cropCenterSquareReturnsSquareBitmapInsideRoi() {
        val bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(80, 120, 160))
        }

        val cropped = RoiCropper().cropCenterSquare(bitmap)
        val expectedBounds = RoiGeometry.calculateImageBounds(bitmap.width, bitmap.height)

        assertNotNull(cropped)
        assertEquals(cropped.width, cropped.height)
        assertEquals(expectedBounds.size, cropped.width)
        assertTrue(cropped.width in 1..480)
    }
}
