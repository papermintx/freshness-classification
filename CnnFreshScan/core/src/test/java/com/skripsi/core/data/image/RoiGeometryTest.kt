package com.skripsi.core.data.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoiGeometryTest {

    @Test
    fun calculateImageBoundsUsesSameFractionAndBiasForPortraitFrame() {
        val bounds = RoiGeometry.calculateImageBounds(
            imageWidth = 1080,
            imageHeight = 1920
        )

        assertEquals(864, bounds.size)
        assertEquals(108, bounds.left)
        assertEquals(454, bounds.top)
        assertEquals(bounds.left + bounds.size, bounds.right)
        assertEquals(bounds.top + bounds.size, bounds.bottom)
    }

    @Test
    fun calculateImageBoundsIsAlwaysInsideSourceImage() {
        val sizes = listOf(
            640 to 480,
            480 to 640,
            224 to 224,
            3024 to 4032
        )

        sizes.forEach { (width, height) ->
            val bounds = RoiGeometry.calculateImageBounds(width, height)

            assertTrue(bounds.left >= 0)
            assertTrue(bounds.top >= 0)
            assertTrue(bounds.right <= width)
            assertTrue(bounds.bottom <= height)
            assertEquals(bounds.size, bounds.right - bounds.left)
            assertEquals(bounds.size, bounds.bottom - bounds.top)
        }
    }
}
