package com.sample.smartremote.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.sample.smartremote.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sample.smartremote.data.RemoteDevice
import com.sample.smartremote.data.RemoteState
import com.sample.smartremote.ui.theme.Primary
import com.sample.smartremote.ui.theme.SurfaceContainer
import com.sample.smartremote.ui.theme.SurfaceContainerHighest
import androidx.compose.ui.tooling.preview.Preview
import com.sample.smartremote.ui.views.DpadView
import com.sample.smartremote.ui.views.NeumorphicButton
import com.sample.smartremote.ui.views.ProtrudingDpad

enum class DPadDirection {
    UP, DOWN, LEFT, RIGHT,NONE
}
@Composable
fun DaznRemoteScreen(
    uiState: RemoteState,
    statusText: String,
    selectedDeviceName: String?,
    onMicClick: () -> Unit,
    onDirectionClick: (DPadDirection) -> Unit,
    onOkClick: () -> Unit,
    onBackClick: () -> Unit,
    onKeyboardClick: () -> Unit,
    onMuteClick: () -> Unit,
    onHeaderClick: () -> Unit,
    onIdentifyClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isListening = uiState is RemoteState.LISTENING || uiState is RemoteState.PROCESSING || uiState is RemoteState.RESULT

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.TopCenter
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            // -- Old Device Selection Header --
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onHeaderClick
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tv,
                        contentDescription = "Select Device",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = selectedDeviceName ?: "Select Device",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Device List",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (selectedDeviceName != null && selectedDeviceName != "All Devices") {
                    Button(
                        onClick = onIdentifyClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.1f),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Identify",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            // -- Dynamic Content Area --
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isListening) {
                    ListeningCard(
                        uiState = uiState,
                        haptic = haptic,
                        onClose = onMicClick
                    )
                } else {
                    DpadView(
                        haptic = haptic,
                        onDirectionClick = onDirectionClick,
                        onOkClick = onOkClick
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // -- Bottom Grid Buttons --
            Column(
                verticalArrangement = Arrangement.spacedBy(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 80.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                    NeumorphicButton(icon = Icons.AutoMirrored.Rounded.ArrowBack, haptic = haptic, onClick = onBackClick, contentDescription = "Back")
                    NeumorphicButton(icon = Icons.Rounded.Keyboard, haptic = haptic, onClick = onKeyboardClick, contentDescription = "Keyboard")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                    NeumorphicButton(icon = Icons.AutoMirrored.Rounded.VolumeOff, haptic = haptic, onClick = onMuteClick, contentDescription = "Mute")
                    NeumorphicButton(
                        icon = Icons.Rounded.Mic,
                        haptic = haptic,
                        onClick = onMicClick,
                        isActive = isListening,
                        contentDescription = "Microphone"
                    )
                }
            }
        }
    }
}

@Composable
fun ListeningCard(uiState: RemoteState, haptic: HapticFeedback, onClose: () -> Unit) {
    val borderGradient = Brush.linearGradient(listOf(Color(0xFF552A8E), Color(0xFFC02C66)))
    val micGradient = Brush.linearGradient(listOf(Color(0xFFB5F057), Color(0xFF45B079)))

    Box(
        modifier = Modifier
            .width(320.dp)
            .height(320.dp)
            .border(1.5.dp, borderGradient, RoundedCornerShape(20.dp))
            .background(Color(0xFF13151A), RoundedCornerShape(20.dp))
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Check your voice prompt below", color = Color(0xFF8A8A93), fontSize = 14.sp)
                Icon(
                    Icons.Rounded.Close, "Close", tint = Color(0xFF8A8A93),
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                            onClose()
                        }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            val text = when (uiState) {
                is RemoteState.LISTENING -> "Listening..."
                is RemoteState.PROCESSING -> "Processing..."
                is RemoteState.RESULT -> uiState.transcript
                else -> ""
            }
            
            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.CenterHorizontally)
                    .shadow(elevation = 4.dp, shape = CircleShape, clip = true)
                    .background(Color(0xFF0D1211), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .background(micGradient, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = "Mic Active",
                        tint = Color(0xFF103020),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF222227), CircleShape)
                        .border(1.dp, Color(0xFF333338), CircleShape)
                        .clickable { haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Delete, "Delete", tint = Color(0xFF8A8A93), modifier = Modifier.size(18.dp))
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .padding(horizontal = 16.dp)
                        .background(Brush.horizontalGradient(listOf(Color(0xFF28656E), Color(0xFF45B079))))
                )

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF4A4A52), CircleShape)
                        .clickable { haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.KeyboardArrowUp, "Send", tint = Color(0xFF13151A), modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSelectionSheet(
    devices: List<RemoteDevice>,
    selectedDeviceId: String?,
    onDeviceSelect: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState
) {
    var deviceToRename by remember { mutableStateOf<RemoteDevice?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceContainer,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        "Select Device",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        if (devices.isEmpty()) "No device available" else "${devices.size} devices available ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(SurfaceContainerHighest, CircleShape)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(devices) { device ->
                    DeviceItem(
                        device = device,
                        isSelected = device.id == selectedDeviceId,
                        onClick = { onDeviceSelect(device.id) },
                        onRenameClick = { deviceToRename = device }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (deviceToRename != null) {
            RenameDialog(
                currentName = deviceToRename?.name ?: "",
                onConfirm = { newName: String ->
                    onRename(deviceToRename!!.id, newName)
                    deviceToRename = null
                },
                onDismiss = { deviceToRename = null }
            )
        }
    }
}

@Composable
fun DeviceItem(
    device: RemoteDevice,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRenameClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) Primary.copy(alpha = 0.15f) else SurfaceContainerHighest,
        shape = RoundedCornerShape(20.dp),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.5f))
        } else null
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Primary else SurfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tv,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    device.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    if (isSelected) "Connected" else "Available",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) Primary else Color.White.copy(alpha = 0.5f)
                )
            }

            if (device.id != "__all__") {
                IconButton(onClick = onRenameClick) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Rename",
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Selected",
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun RenameDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentName) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Device", color = Color.White) },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = SurfaceContainerHighest,
                    unfocusedContainerColor = SurfaceContainerHighest,
                    cursorColor = Primary
                ),
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text("Save", color = Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.6f))
            }
        },
        containerColor = SurfaceContainer,
        textContentColor = Color.White,
        titleContentColor = Color.White
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun DaznRemoteScreenPreview() {
    DaznRemoteScreen(
        uiState = RemoteState.IDLE,
        statusText = "Play Red Bull TV",
        selectedDeviceName = "Living Room TV",
        onMicClick = {},
        onDirectionClick = {},
        onOkClick = {},
        onBackClick = {},
        onKeyboardClick = {},
        onMuteClick = {},
        onHeaderClick = {},
        onIdentifyClick = {}
    )
}
