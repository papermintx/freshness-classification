package com.skripsi.core.data.image

import android.graphics.Bitmap
import javax.inject.Inject
import com.skripsi.core.data.image.RoiGeometry.CAMERA_ROI_VERTICAL_BIAS
import com.skripsi.core.data.image.RoiGeometry.CENTER_ROI_FRACTION

class RoiCropper @Inject constructor() {
    fun cropCenterSquare(
        bitmap: Bitmap,
        sizeFraction: Float = CENTER_ROI_FRACTION,
        verticalBias: Float = CAMERA_ROI_VERTICAL_BIAS
    ): Bitmap {
        val bounds = RoiGeometry.calculateImageBounds(
            imageWidth = bitmap.width,
            imageHeight = bitmap.height,
            sizeFraction = sizeFraction,
            verticalBias = verticalBias
        )

        return Bitmap.createBitmap(
            bitmap,
            bounds.left,
            bounds.top,
            bounds.size,
            bounds.size
        )
    }

    companion object {
        const val CENTER_ROI_FRACTION = RoiGeometry.CENTER_ROI_FRACTION
        const val CAMERA_ROI_VERTICAL_BIAS = RoiGeometry.CAMERA_ROI_VERTICAL_BIAS
    }
}
