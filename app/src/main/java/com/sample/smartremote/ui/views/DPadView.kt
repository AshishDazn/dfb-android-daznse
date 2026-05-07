package com.sample.smartremote.ui.views

import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sample.smartremote.ui.screens.DPadDirection
import kotlin.math.abs
import kotlin.math.sqrt

fun getTouchedQuadrant(offset: Offset, size: Float): DPadDirection {
    val center = size / 2
    val x = offset.x - center
    val y = offset.y - center

    // Check if touch is within the circular bounds
    val radius = size / 2
    if (sqrt(x * x + y * y) > radius) return DPadDirection.NONE

    return when {
        abs(x) > abs(y) -> if (x > 0) DPadDirection.RIGHT else DPadDirection.LEFT
        else -> if (y > 0) DPadDirection.DOWN else DPadDirection.UP
    }
}

@Composable
fun DpadView(
    haptic: HapticFeedback,
    onDirectionClick: (DPadDirection) -> Unit,
    onOkClick: () -> Unit) {

    val dpadSize = 260.dp
    val okSize = 90.dp
    var activeQuadrant by remember { mutableStateOf(DPadDirection.NONE) }

    // Animation for the "pressed" depth (the button sinks slightly)
    val elevation by animateDpAsState(if (activeQuadrant != DPadDirection.NONE) 2.dp else 12.dp)
    val scale by animateFloatAsState(if (activeQuadrant != DPadDirection.NONE) 0.98f else 1f)
    // We use a lighter alpha for a subtle "shadow" feel
    val shadowIntensity by animateFloatAsState(
        targetValue = if (activeQuadrant != DPadDirection.NONE) 0.4f else 0f,
        animationSpec = tween(100),
        label = "PressIntensity"
    )

    Box(
        modifier = Modifier
            .size(dpadSize)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .shadow(elevation = elevation, shape = CircleShape, clip = false)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val touched = getTouchedQuadrant(offset, size.width.toFloat())
                        if (touched != DPadDirection.NONE) {
                            activeQuadrant = touched
                            onDirectionClick(activeQuadrant)
                        }
                        tryAwaitRelease()
                        activeQuadrant = DPadDirection.NONE
                    }
                )
            }
            .drawBehind {
                val radius = size.width / 2

                // 1. Protruding (Convex) Gradient
                // Light source from Top-Left: Lighter grey to Darker grey
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF3A3A3A), Color(0xFF121212)),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    )
                )

                // 2. High-End Rim Highlight (Bevel effect)
                drawCircle(
                    color = Color.Black,
                    style = Stroke(width = 3.dp.toPx()),
                    radius = radius - 1.dp.toPx()
                )

                // 3. 130-Degree "Shadow" Arc (Simulates the press/tilt)
                if (activeQuadrant != DPadDirection.NONE) {
                    val sweep = 130f
                    val halfSweep = sweep / 2
                    val startAngle = when (activeQuadrant) {
                        DPadDirection.RIGHT -> 0f - halfSweep
                        DPadDirection.DOWN -> 90f - halfSweep
                        DPadDirection.LEFT -> 180f - halfSweep
                        DPadDirection.UP -> 270f - halfSweep
                        else -> 0f
                    }

                    // Instead of Color.Black, we use a Gradient to create "Depth"
                    // This makes the section look like it has tilted inward
                    drawArc(
                        brush = Brush.radialGradient(
                            0.0f to Color.Black.copy(alpha = shadowIntensity),
                            1.0f to Color.Transparent,
                            center = center,
                            radius = radius
                        ),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = true
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Directional Icons (layered on top)
        DPadIcons(activeQuadrant)

        // Raised Center OK Button
        Box(
            modifier = Modifier
                .size(110.dp)
                .shadow(elevation = 6.dp, shape = CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF333333), Color(0xFF1A1A1A))
                    ),
                    shape = CircleShape
                )
                .border(1.dp, Color.Black, CircleShape)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOkClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "OK",
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun DPadIcons(active: DPadDirection) {
    val inactiveAlpha = 0.5f
    val activeAlpha = 1f

    Box(modifier = Modifier.fillMaxSize()) {
        Icon(
            Icons.Default.KeyboardArrowUp, null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            tint = Color.White.copy(alpha = if (active == DPadDirection.UP) activeAlpha else inactiveAlpha)
        )
        Icon(
            Icons.Default.KeyboardArrowDown, null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            tint = Color.White.copy(alpha = if (active == DPadDirection.DOWN) activeAlpha else inactiveAlpha)
        )
        Icon(
            Icons.Default.KeyboardArrowLeft, null,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp),
            tint = Color.White.copy(alpha = if (active == DPadDirection.LEFT) activeAlpha else inactiveAlpha)
        )
        Icon(
            Icons.Default.KeyboardArrowRight, null,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            tint = Color.White.copy(alpha = if (active == DPadDirection.RIGHT) activeAlpha else inactiveAlpha)
        )
    }
}