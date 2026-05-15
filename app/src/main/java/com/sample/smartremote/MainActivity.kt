package com.sample.smartremote

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sample.smartremote.ui.screens.AuthorizationScreen
import com.sample.smartremote.ui.screens.DPadDirection
import com.sample.smartremote.ui.screens.DaznRemoteScreen
import com.sample.smartremote.ui.screens.DeviceSelectionSheet
import com.sample.smartremote.ui.theme.SmartRemoteTheme

import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission granted
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            SmartRemoteTheme {
                SmartRemoteAppMain()
            }
        }
    }
}

@Composable
fun SmartRemoteAppMain() {
    val authViewModel: AuthViewModel = koinViewModel()
    val isAuthorized by authViewModel.isAuthorized.collectAsState()
    val isLoggingIn by authViewModel.isLoggingIn.collectAsState()
    val loginError by authViewModel.loginError.collectAsState()

    if (!isAuthorized) {
        AuthorizationScreen(
            onSignIn = { email, password -> authViewModel.signIn(email, password) },
            isLoading = isLoggingIn,
            errorMessage = loginError
        )
    } else {
        val remoteViewModel: RemoteViewModel = koinViewModel()
        SmartRemoteContent(remoteViewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartRemoteContent(viewModel: RemoteViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val selectedDeviceId by viewModel.selectedDeviceId.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    var showDeviceSheet by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.connect()
            } else if (event == Lifecycle.Event.ON_STOP) {
                viewModel.disconnect()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        verifyAppPermissions()
    }

    private fun verifyAppPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}
