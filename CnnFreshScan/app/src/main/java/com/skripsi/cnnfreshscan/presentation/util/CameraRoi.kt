package com.skripsi.cnnfreshscan.presentation.util

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.ui.geometry.Rect
import kotlin.math.min
import kotlin.math.roundToInt

fun Bitmap.rotateClockwise(rotationDegrees: Int): Bitmap {
    val normalizedRotation = ((rotationDegrees % 360) + 360) % 360
    if (normalizedRotation == 0) return this

    val matrix = Matrix().apply {
        postRotate(normalizedRotation.toFloat())
    }

    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun mapCenterCropRoiToPreviewSide(
    viewWidth: Float,
    viewHeight: Float,
    imageWidth: Int,
    imageHeight: Int,
    sizeFraction: Float
): Float {
    if (viewWidth <= 0f || viewHeight <= 0f || imageWidth <= 0 || imageHeight <= 0) {
        return min(viewWidth, viewHeight) * sizeFraction
    }

    // Hitung ROI size berdasarkan ukuran terkecil dari image
    val imageShortestSide = min(imageWidth, imageHeight).toFloat()
    val cropSizeInImage = imageShortestSide * sizeFraction

    // Scale ke ukuran view
    val viewShortestSide = min(viewWidth, viewHeight)
    val scale = viewShortestSide / imageShortestSide

    return cropSizeInImage * scale
}

fun stableCenteredRoiRect(
    viewWidth: Float,
    viewHeight: Float,
    sizeFraction: Float,
    verticalBias: Float
): Rect {
    val safeWidth = viewWidth.coerceAtLeast(0f)
    val safeHeight = viewHeight.coerceAtLeast(0f)
    val side = min(safeWidth, safeHeight) * sizeFraction.coerceIn(0.1f, 1f)
    val left = (safeWidth - side) / 2f
    val centeredTop = ((safeHeight - side) / 2f).roundToInt().toFloat()
    val maxShift = (safeHeight - side).coerceAtLeast(0f) / 2f
    val top = (centeredTop + verticalBias.coerceIn(-0.45f, 0.45f) * maxShift)
        .coerceIn(0f, safeHeight - side)

    return Rect(left, top, left + side, top + side)
}
