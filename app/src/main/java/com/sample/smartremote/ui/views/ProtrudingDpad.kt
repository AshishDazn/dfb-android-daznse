package com.sample.smartremote.ui.views

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sample.smartremote.ui.screens.DPadDirection

@Composable
fun ProtrudingDpad(
    haptic: HapticFeedback,
    onDirectionClick: (DPadDirection) -> Unit,
    onOkClick: () -> Unit
) {
    val dpadSize = 260.dp
    val okSize = 90.dp

    Box(modifier = Modifier.size(dpadSize), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(24.dp, CircleShape, spotColor = Color.Black)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF2A2A30), Color(0xFF19191D)),
                        center = Offset(0.3f, 0.3f)
                    ),
                    CircleShape
                )
                .border(2.dp, Color(0xFF4A4A52).copy(alpha = 0.3f), CircleShape)
        )

        Box(modifier = Modifier.fillMaxSize()) {
            DpadQuadrant(
                modifier = Modifier.align(Alignment.TopCenter),
                icon = Icons.Rounded.KeyboardArrowUp,
                contentDescription = "Up",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                    onDirectionClick(DPadDirection.UP)
                }
            )
            DpadQuadrant(
                modifier = Modifier.align(Alignment.BottomCenter),
                icon = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Down",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                    onDirectionClick(DPadDirection.DOWN)
                }
            )
            DpadQuadrant(
                modifier = Modifier.align(Alignment.CenterStart),
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = "Left",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                    onDirectionClick(DPadDirection.LEFT)
                }
            )
            DpadQuadrant(
                modifier = Modifier.align(Alignment.CenterEnd),
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "Right",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                    onDirectionClick(DPadDirection.RIGHT)
                }
            )
        }

        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "ok_scale")

        Box(
            modifier = Modifier
                .size(okSize)
                .scale(scale)
                .shadow(16.dp, CircleShape, spotColor = Color.Black)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF2A2A30), Color(0xFF15151A))
                    ),
                    CircleShape
                )
                .border(1.dp, Color(0xFF3A3A42), CircleShape)
                .clip(CircleShape)
                .clickable(interactionSource = interactionSource, indication = null) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOkClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "OK",
                color = Color(0xFF8A8A93),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun DpadQuadrant(
    modifier: Modifier,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.85f else 1f, label = "dpad_scale")
    val backgroundColor by animateColorAsState(
        if (isPressed) Color.White.copy(alpha = 0.1f) else Color.Transparent,
        label = "dpad_color"
    )

    Box(
        modifier = modifier
            .size(100.dp)
            .scale(scale)
            .background(backgroundColor, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color(0xFFE2E2E2),
            modifier = Modifier.size(32.dp)
        )
    }
}