package com.sample.smartremote

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sample.smartremote.ui.screens.DPadDirection
import com.sample.smartremote.ui.screens.DaznRemoteScreen
import com.sample.smartremote.ui.screens.DeviceSelectionSheet
import com.sample.smartremote.ui.theme.SmartRemoteTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    val selectedDevice = devices.find { it.id == selectedDeviceId }
    val haptic = LocalHapticFeedback.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black, // Handled by BackgroundLayered inside DaznRemoteScreen
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            DaznRemoteScreen(
                uiState = uiState,
                statusText = statusText,
                selectedDeviceName = selectedDevice?.name,
                onMicClick = {
                    if (selectedDeviceId == null) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please select a device first")
                        }
                    } else if (!hasPermission) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        viewModel.toggleListening(selectedDeviceId)
                    }
                },
                onDirectionClick = { direction ->
                    val icon = when (direction) {
                        DPadDirection.UP -> Icons.Rounded.KeyboardArrowUp
                        DPadDirection.DOWN -> Icons.Rounded.KeyboardArrowDown
                        DPadDirection.LEFT -> Icons.AutoMirrored.Rounded.KeyboardArrowLeft
                        DPadDirection.RIGHT -> Icons.AutoMirrored.Rounded.KeyboardArrowRight
                    }
                    viewModel.sendRemoteAction(icon)
                },
                onOkClick = {
                    viewModel.sendRemoteAction(Icons.Rounded.Check)
                },
                onBackClick = {
                    viewModel.sendRemoteAction(Icons.AutoMirrored.Rounded.ArrowBack)
                },
                onKeyboardClick = {
                    // Logic for keyboard
                },
                onMuteClick = {
                    viewModel.sendRemoteAction(Icons.AutoMirrored.Rounded.VolumeOff)
                },
                onHeaderClick = {
                    showDeviceSheet = true
                },
                onIdentifyClick = {
                    if (selectedDeviceId != null && selectedDeviceId != "__all__") {
                        viewModel.identifyDevice(selectedDeviceId!!)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
            )
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

// Ensure DeviceSelectionSheet and RenameDialog are still accessible (moved from bottom of old MainActivity)
// I'll keep them in MainActivity for now but they should ideally be in ui.screens
