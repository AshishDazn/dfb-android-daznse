package com.sample.smartremote.ui.views

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun WaveformAnimation(isAnimating: Boolean, modifier: Modifier) {
    val staticHeights = remember {
        listOf(
            0.2f,
            0.4f,
            0.3f,
            0.6f,
            0.4f,
            0.8f,
            0.5f,
            1f,
            0.5f,
            0.8f,
            0.4f,
            0.6f,
            0.3f,
            0.4f,
            0.2f
        )
    }
    Row(
        modifier = modifier.height(20.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "waveform")

        repeat(15) { index ->
            val duration = 400 + (index * 50) % 300
            val height by if (isAnimating) {
                infiniteTransition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(duration, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "height"
                )
            } else {
                remember { mutableStateOf(staticHeights[index]) }
            }

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(height)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(Color(0xFF28656E), Color(0xFF45B079))))
            )
        }
    }
}