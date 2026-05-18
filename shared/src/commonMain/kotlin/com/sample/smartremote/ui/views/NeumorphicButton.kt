package com.sample.smartremote.ui.views

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp

@Composable
fun NeumorphicButton(
    icon: ImageVector,
    haptic: HapticFeedback,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isEnabled: Boolean = true,
    isActive: Boolean = false,
    contentDescription: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        label = "btn_scale"
    )

    val outerModifier = if (isActive) {
        Modifier.border(2.dp, Color(0xFF3B82F6), CircleShape)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .scale(scale)
            .size(72.dp)
            .then(outerModifier)
            .shadow(12.dp, CircleShape, spotColor = Color.Black)
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFF2A2A30), Color(0xFF19191D)), center = Offset(0.3f, 0.3f)
                ), CircleShape
            )
            .border(1.dp, Color.Black, CircleShape)
            .clip(CircleShape)
            .clickable(
                enabled = isEnabled,
                interactionSource = interactionSource,
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) Color(0xFF3B82F6) else Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}
