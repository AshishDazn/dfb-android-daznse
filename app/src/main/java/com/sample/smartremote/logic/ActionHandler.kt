package com.sample.smartremote.logic

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.sample.smartremote.data.SocketEventsHelper

class ActionHandler {

    fun mapVoiceCommandToIcon(transcript: String): ImageVector? {
        val command = transcript.lowercase()
        return when {
            command.contains("home") -> Icons.Rounded.Home
            command.contains("settings") -> Icons.Rounded.Settings
            command.contains("volume up") || command.contains("increase volume") -> Icons.AutoMirrored.Rounded.VolumeUp
            command.contains("volume down") || command.contains("decrease volume") -> Icons.AutoMirrored.Rounded.VolumeDown
            command.contains("mute") -> Icons.AutoMirrored.Rounded.VolumeOff
            command.contains("back") -> Icons.AutoMirrored.Rounded.ArrowBack
            command.contains("ok") || command.contains("select") -> Icons.Rounded.Check
            command.contains("up") -> Icons.Rounded.KeyboardArrowUp
            command.contains("down") -> Icons.Rounded.KeyboardArrowDown
            command.contains("left") -> Icons.AutoMirrored.Rounded.KeyboardArrowLeft
            command.contains("right") -> Icons.AutoMirrored.Rounded.KeyboardArrowRight
            command.contains("schedule") -> Icons.Rounded.Schedule
            else -> null
        }
    }

    fun mapIconToCommand(icon: ImageVector?): String? {
        return when (icon) {
            Icons.Rounded.Home -> SocketEventsHelper.Home
            Icons.Rounded.Settings -> SocketEventsHelper.Settings
            Icons.AutoMirrored.Rounded.VolumeDown -> SocketEventsHelper.VolumeDown
            Icons.AutoMirrored.Rounded.VolumeUp -> SocketEventsHelper.VolumeUp
            Icons.Rounded.Schedule -> SocketEventsHelper.Schedule
            Icons.Rounded.KeyboardArrowUp -> SocketEventsHelper.Up
            Icons.Rounded.KeyboardArrowDown -> SocketEventsHelper.Down
            Icons.AutoMirrored.Rounded.KeyboardArrowLeft -> SocketEventsHelper.Left
            Icons.AutoMirrored.Rounded.KeyboardArrowRight -> SocketEventsHelper.Right
            Icons.Rounded.Check -> SocketEventsHelper.Ok
            Icons.AutoMirrored.Rounded.ArrowBack -> SocketEventsHelper.Back
            Icons.AutoMirrored.Rounded.VolumeOff -> SocketEventsHelper.Mute
            else -> null
        }
    }
}
