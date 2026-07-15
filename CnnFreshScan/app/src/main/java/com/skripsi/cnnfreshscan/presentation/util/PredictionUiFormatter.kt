package com.skripsi.cnnfreshscan.presentation.util

import java.util.Locale

const val DEFAULT_PREDICTION_LABEL = "Menunggu Hasil"

private val produceDisplayNames = mapOf(
    "apple" to "APEL",
    "banana" to "PISANG",
    "carrot" to "WORTEL",
    "cucumber" to "TIMUN",
    "mango" to "MANGGA",
    "orange" to "JERUK",
    "potato" to "KENTANG",
    "tomato" to "TOMAT"
)

fun toDisplayLabel(rawLabel: String?, confidence: Float): String {
    if (rawLabel.isNullOrBlank()) {
        return DEFAULT_PREDICTION_LABEL
    }

    val parts = rawLabel.split("_").map { it.lowercase(Locale.ROOT) }
    val conditionToken = parts.firstOrNull { it == "fresh" || it == "rotten" }
    val produceToken = parts.lastOrNull()

    val productName = produceDisplayNames[produceToken.orEmpty()]
        ?: produceToken?.replaceFirstChar { it.uppercase() }?.uppercase(Locale.ROOT)
        ?: rawLabel.replace("_", " ").uppercase(Locale.ROOT)

    val condition = when (conditionToken) {
        "fresh" -> "SEGAR"
        "rotten" -> "BUSUK"
        else -> ""
    }

    return listOf(productName, condition)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { rawLabel.replace("_", " ").uppercase(Locale.ROOT) }
}

fun formatAccuracy(confidence: Float): String {
    return String.format(Locale.US, "%.1f%%", confidence.coerceIn(0f, 1f) * 100f)
}

fun buildHandlingAdvice(rawLabel: String?, confidence: Float): String {
    if (rawLabel.isNullOrBlank()) {
        return "Pastikan objek berada di dalam area scan, pencahayaan cukup, dan gambar tidak buram sebelum mencoba lagi."
    }

    val parts = rawLabel.split("_").map { it.lowercase(Locale.ROOT) }
    return when (parts.firstOrNull { it == "fresh" || it == "rotten" }) {
        "fresh" -> "Kualitas baik. Simpan di kulkas agar tahan lebih lama."
        "rotten" -> "Kondisi kurang baik. Pisahkan dari bahan lain dan segera buang bagian yang rusak."
        else -> "Simpan produk di tempat yang bersih dan cek kembali kondisinya secara berkala."
    }
}

fun buildWatermarkText(displayLabel: String, confidence: Float): String {
    return "$displayLabel - ${formatAccuracy(confidence)}"
}
