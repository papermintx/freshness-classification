package com.skripsi.cnnfreshscan.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.skripsi.cnnfreshscan.ui.theme.CnnFreshScanTheme
import com.skripsi.cnnfreshscan.ui.theme.FreshAccentBlue
import com.skripsi.cnnfreshscan.ui.theme.FreshSurfaceTint
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        delay(1600)
        onFinished()
    }

    SplashScreenContent(modifier = modifier)
}

@Composable
private fun SplashScreenContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SplashIconTile(
                    backgroundColor = FreshSurfaceTint,
                    iconColor = MaterialTheme.colorScheme.primary,
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Eco,
                            contentDescription = null,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                )
                SplashIconTile(
                    backgroundColor = FreshSurfaceTint.copy(alpha = 0.95f),
                    iconColor = FreshAccentBlue,
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.DocumentScanner,
                            contentDescription = null,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "CNN Fresh Scan",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun SplashIconTile(
    backgroundColor: Color,
    iconColor: Color,
    icon: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(82.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(backgroundColor)
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides iconColor) {
            icon()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashScreenPreview() {
    CnnFreshScanTheme(darkTheme = false) {
        SplashScreenContent()
    }
}