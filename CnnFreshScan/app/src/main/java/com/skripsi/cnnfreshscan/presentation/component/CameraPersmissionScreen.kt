package com.skripsi.cnnfreshscan.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun CameraPermissionScreen(
    onGrantPermission: () -> Unit,
    isDenied: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon Kamera Gede di tengah
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = "Camera Icon",
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Izin Kamera Dibutuhkan",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Teks penjelasan yang beda kalau sempet ditolak
        Text(
            text = if (isDenied) {
                "SegarCek butuh akses kamera nih buat nge-scan kesegaran buah dan sayur kamu. Yuk, kasih izin dulu biar aplikasinya bisa dipake!"
            } else {
                "Mohon berikan akses kamera untuk mulai menggunakan fitur deteksi kesegaran."
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Tombol buat memicu launcher izin yang ada di CameraScreen
        Button(
            onClick = onGrantPermission,
            modifier = Modifier.height(50.dp)
        ) {
            Text(text = "Berikan Izin Kamera")
        }
    }
}