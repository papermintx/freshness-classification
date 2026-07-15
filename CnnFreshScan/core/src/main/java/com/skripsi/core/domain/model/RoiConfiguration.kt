package com.skripsi.core.domain.model

data class RoiConfiguration(
    val sizeFraction: Float,
    val verticalBias: Float
) {
    companion object {
        val Default = RoiConfiguration(
            sizeFraction = 0.8f,
            verticalBias = -0.14f
        )
    }
}
