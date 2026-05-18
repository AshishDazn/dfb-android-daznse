package com.sample.smartremote.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DaznBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    )
}

@Composable
fun DpadBackgroundGlow(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .drawBehind {
                val center = Offset(size.width / 2, size.height / 2)
                
                // Gold Glow - Brighter and Larger
                drawCircle(
                    brush = Brush.radialGradient(
                        0.0f to Color(0xFFF7931E).copy(alpha = 0.8f), // Brighter Gold
                        0.4f to Color(0xFF866022).copy(alpha = 0.5f),
                        1.0f to Color.Transparent,
                        center = center.copy(x = center.x - 40.dp.toPx()),
                        radius = 190.dp.toPx()
                    ),
                    center = center.copy(x = center.x - 40.dp.toPx()),
                    radius = 190.dp.toPx()
                )
                
                // Purple Glow - Brighter and Larger
                drawCircle(
                    brush = Brush.radialGradient(
                        0.0f to Color(0xFFD03EF0).copy(alpha = 0.8f), // Brighter Purple
                        0.4f to Color(0xFF7B4397).copy(alpha = 0.5f),
                        1.0f to Color.Transparent,
                        center = center.copy(x = center.x + 40.dp.toPx()),
                        radius = 190.dp.toPx()
                    ),
                    center = center.copy(x = center.x + 40.dp.toPx()),
                    radius = 190.dp.toPx()
                )
            }
    )
}
