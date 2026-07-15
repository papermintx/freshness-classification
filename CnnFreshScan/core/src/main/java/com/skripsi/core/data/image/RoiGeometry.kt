package com.skripsi.core.data.image

import kotlin.math.min
import kotlin.math.roundToInt

data class RoiBounds(
    val left: Int,
    val top: Int,
    val size: Int
) {
    val right: Int get() = left + size
    val bottom: Int get() = top + size
}

object RoiGeometry {
    const val CENTER_ROI_FRACTION = 0.8f
    const val CAMERA_ROI_VERTICAL_BIAS = -0.14f

    fun calculateImageBounds(
        imageWidth: Int,
        imageHeight: Int,
        sizeFraction: Float = CENTER_ROI_FRACTION,
        verticalBias: Float = CAMERA_ROI_VERTICAL_BIAS
    ): RoiBounds {
        val safeWidth = imageWidth.coerceAtLeast(1)
        val safeHeight = imageHeight.coerceAtLeast(1)
        val normalizedFraction = sizeFraction.coerceIn(0.1f, 1f)

        val cropSize = (min(safeWidth, safeHeight) * normalizedFraction)
            .roundToInt()
            .coerceIn(1, min(safeWidth, safeHeight))

        val left = ((safeWidth - cropSize) / 2f)
            .roundToInt()
            .coerceIn(0, safeWidth - cropSize)
        val centeredTop = ((safeHeight - cropSize) / 2f)
            .roundToInt()
            .coerceIn(0, safeHeight - cropSize)

        val maxShift = ((safeHeight - cropSize).coerceAtLeast(0)) / 2f

        val top = (centeredTop + verticalBias.coerceIn(-0.45f, 0.45f) * maxShift)
            .roundToInt()
            .coerceIn(0, safeHeight - cropSize)

        return RoiBounds(left = left, top = top, size = cropSize)
    }
}
