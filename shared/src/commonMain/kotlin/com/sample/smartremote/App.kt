package com.sample.smartremote

import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.sample.smartremote.ui.screens.*
import dev.icerock.moko.mvvm.compose.getViewModel
import dev.icerock.moko.mvvm.compose.viewModelFactory
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SmartRemoteApp(viewModel: RemoteViewModel = getViewModel(Unit, viewModelFactory { RemoteViewModel(AudioService()) })) {

    val isAuthorized by viewModel.isAuthorized.collectAsState()
    val isLoggingIn by viewModel.isLoggingIn.collectAsState()
    val loginError by viewModel.loginError.collectAsState()

    CompositionLocalProvider(LocalIndication provides null) {
        if (!isAuthorized) {
            AuthorizationScreen(
                onSignIn = { email, password -> viewModel.signIn(email, password) },
                isLoading = isLoggingIn,
                errorMessage = loginError
            )
        } else {
            SmartRemoteContent(viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartRemoteContent(viewModel: RemoteViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val selectedDeviceId by viewModel.selectedDeviceId.collectAsState()

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    var showDeviceSheet by remember { mutableStateOf(false) }

    val selectedDevice = devices.find { it.id == selectedDeviceId }

    DaznRemoteScreen(
        uiState = uiState,
        selectedDeviceName = selectedDevice?.name,
        onMicClick = {
            selectedDeviceId?.let {
                viewModel.toggleListening(it)
            }
        },
        onDirectionClick = { direction ->
            val action = when (direction) {
                DPadDirection.UP -> "Up"
                DPadDirection.DOWN -> "Down"
                DPadDirection.LEFT -> "Left"
                DPadDirection.RIGHT -> "Right"
                DPadDirection.NONE -> ""
            }
            if (action.isNotEmpty()) viewModel.sendRemoteAction(action)
        },
        onOkClick = { viewModel.sendRemoteAction("Ok") },
        onBackClick = { viewModel.sendRemoteAction("Back") },
        onHomeClick = { viewModel.sendRemoteAction("Home") },
        onMuteClick = { viewModel.sendRemoteAction("Mute") },
        onHeaderClick = { showDeviceSheet = true },
        onIdentifyClick = {
            selectedDeviceId?.let { viewModel.identifyDevice(it) }
        }
    )

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
            onRename = { id, name -> viewModel.renameDevice(id, name) },
            onDismiss = { showDeviceSheet = false },
            sheetState = sheetState
        )
    }
}
