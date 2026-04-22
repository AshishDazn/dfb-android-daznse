package com.sample.smartremote

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sample.smartremote.data.RemoteDevice
import com.sample.smartremote.data.RemoteState
import com.sample.smartremote.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        var keepSplashScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        
        lifecycleScope.launch {
            delay(3000)
            keepSplashScreen = false
        }

        enableEdgeToEdge()
        setContent {
            SmartRemoteTheme {
                SmartRemoteApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartRemoteApp(viewModel: RemoteViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val statusText by viewModel.statusText.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val selectedDeviceId by viewModel.selectedDeviceId.collectAsState()

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    var showDeviceSheet by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                viewModel.connect()
            } else if (event == Lifecycle.Event.ON_STOP) {
                viewModel.disconnect()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    val selectedDevice = devices.find { it.id == selectedDeviceId }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkSurface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            showDeviceSheet = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tv,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            (selectedDevice?.name ?: "Select Device"),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                actions = {
                    if (selectedDeviceId != null && selectedDeviceId != "__all__") {
                        Button(
                            onClick = {
                                viewModel.identifyDevice(selectedDeviceId!!)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Primary.copy(alpha = 0.15f),
                                contentColor = Primary
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                "Identify",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Main Voice Section
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = when(uiState){
                        is RemoteState.IDLE -> "Press mic to start voice command"
                        is RemoteState.LISTENING -> "Listening"
                        is RemoteState.PROCESSING -> "Processing"
                        is RemoteState.ERROR -> (uiState as? RemoteState.ERROR)?.message?: "Something went wrong"
                        is RemoteState.RESULT -> viewModel.getExtractedMessage(uiState as? RemoteState.RESULT)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp ,0.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Animated Mic Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(160.dp)
                ) {
                    if (uiState is RemoteState.LISTENING) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.4f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "scale"
                        )
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.6f,
                            targetValue = 0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "alpha"
                        )

                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    this.alpha = alpha
                                }
                                .background(Primary, CircleShape)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Primary, PrimaryDim)
                                )
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (selectedDeviceId == null) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Please select a device first")
                                    }
                                    return@clickable
                                }
                                if (!hasPermission) {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    if (uiState is RemoteState.IDLE || uiState is RemoteState.ERROR) {
                                        viewModel.toggleListening(selectedDeviceId)
                                    } else if (uiState is RemoteState.LISTENING) {
                                        viewModel.toggleListening(selectedDeviceId)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = "Mic",
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(60.dp))

                // Waveform
                WaveformAnimation(isAnimating = uiState is RemoteState.LISTENING)
            }

            // Quick Actions Card
            QuickActionsCard(viewModel)

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showDeviceSheet) {
            DeviceSelectionSheet(
                devices = devices,
                selectedDeviceId = selectedDeviceId,
                onDeviceSelect = { id ->
                    viewModel.selectDevice(id)
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showDeviceSheet = false
                    }
                },
                onRename = { id, name ->
                    viewModel.renameDevice(id, name)
                           },
                onDismiss = { showDeviceSheet = false },
                sheetState = sheetState
            )
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

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                onConfirm = { newName ->
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
fun WaveformAnimation(isAnimating: Boolean) {
    val staticHeights = remember {
        listOf(0.2f, 0.4f, 0.3f, 0.6f, 0.4f, 0.8f, 0.5f, 1f, 0.5f, 0.8f, 0.4f, 0.6f, 0.3f, 0.4f, 0.2f)
    }
    Row(
        modifier = Modifier.height(30.dp),
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
                    .background(Primary.copy(alpha = 0.7f))
            )
        }
    }
}

@Composable
fun QuickActionsCard(viewModel: RemoteViewModel) {
    Card(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "QUICK ACTIONS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                ),
                color = Color.White.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                val actions = listOf(
                    Icons.Rounded.Home to "Home",
                    Icons.AutoMirrored.Rounded.VolumeDown to "Vol Down",
                    Icons.Rounded.Schedule to "Schedule",
                    Icons.AutoMirrored.Rounded.VolumeUp to "Vol Up",
                    Icons.Rounded.Settings to "Settings"
                )

                actions.forEach { (icon, label) ->
                    QuickActionButton(
                        icon = icon,
                        label = label
                    ) {
                        // Action logic here
                        viewModel.sendRemoteAction(icon)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(200)
            isPressed = false
        }
    }

    val activeColor = Primary
    val inactiveColor = SurfaceContainerHighest

    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed) activeColor else inactiveColor,
        animationSpec = tween(durationMillis = 200),
        label = "bgColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isPressed) Color.White else Color.White,
        animationSpec = tween(durationMillis = 200),
        label = "contentColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(backgroundColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    isPressed = true
                    onClick()
                }
                .drawBehind {
                    if (isPressed) {
                        drawCircle(
                            color = activeColor.copy(alpha = 0.25f),
                            radius = size.maxDimension * 0.8f
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = if (isPressed) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isPressed) activeColor else Color.White.copy(alpha = 0.5f)
        )
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
