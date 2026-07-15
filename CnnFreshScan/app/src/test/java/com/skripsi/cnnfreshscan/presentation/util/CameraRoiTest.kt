package com.skripsi.cnnfreshscan.presentation.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraRoiTest {

    @Test
    fun stableCenteredRoiRectStaysSquareAndUsesSameVerticalBiasInPortraitAndLandscape() {
        val portraitRect = stableCenteredRoiRect(
            viewWidth = 1080f,
            viewHeight = 1920f,
            sizeFraction = 0.8f,
            verticalBias = -0.14f
        )
        val landscapeRect = stableCenteredRoiRect(
            viewWidth = 1920f,
            viewHeight = 1080f,
            sizeFraction = 0.8f,
            verticalBias = -0.14f
        )

        assertEquals(864f, portraitRect.width, 0.01f)
        assertEquals(864f, portraitRect.height, 0.01f)
        assertEquals(108f, portraitRect.left, 0.01f)
        assertEquals(454.08f, portraitRect.top, 0.01f)

        assertEquals(864f, landscapeRect.width, 0.01f)
        assertEquals(864f, landscapeRect.height, 0.01f)
        assertEquals(528f, landscapeRect.left, 0.01f)
        assertEquals(92.88f, landscapeRect.top, 0.01f)
    }
}
