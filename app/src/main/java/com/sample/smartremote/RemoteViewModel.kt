package com.sample.smartremote

import android.util.Log
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.sample.smartremote.data.RemoteDevice
import com.sample.smartremote.data.RemoteState
import com.sample.smartremote.data.SocketEventsHelper
import com.sample.smartremote.data.SocketEventsHelper.Back
import com.sample.smartremote.data.SocketEventsHelper.Down
import com.sample.smartremote.data.SocketEventsHelper.EVENT_TV_LIST
import com.sample.smartremote.data.SocketEventsHelper.Home
import com.sample.smartremote.data.SocketEventsHelper.Left
import com.sample.smartremote.data.SocketEventsHelper.Mute
import com.sample.smartremote.data.SocketEventsHelper.Ok
import com.sample.smartremote.data.SocketEventsHelper.Right
import com.sample.smartremote.data.SocketEventsHelper.Schedule
import com.sample.smartremote.data.SocketEventsHelper.Settings
import com.sample.smartremote.data.SocketEventsHelper.Up
import com.sample.smartremote.data.SocketEventsHelper.VolumeDown
import com.sample.smartremote.data.SocketEventsHelper.VolumeUp
import com.sample.smartremote.data.WebSocketResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class RemoteViewModel : ViewModel() {
    private val audioService = AudioService()
    private val webSocketService = WebSocketService()
    private val gson = Gson()

    private val _uiState = MutableStateFlow<RemoteState>(RemoteState.IDLE)
    val uiState = _uiState.asStateFlow()

    private val _devices = MutableStateFlow<List<RemoteDevice>>(emptyList())
    val devices = _devices.asStateFlow()

    private val _selectedDeviceId = MutableStateFlow<String?>(null)
    val selectedDeviceId = _selectedDeviceId.asStateFlow()

    private var isManualDisconnect = false

    private val wsListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d("WebSocket", "Connected")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d("WebSocket", "Message: $text")
            try {
                val response = gson.fromJson(text, WebSocketResponse::class.java)
                
                if (response.type == "final_transcript") {
                    val transcript = getExtractedMessage(response.transcript)
                    _uiState.value = RemoteState.RESULT(transcript)
                    
                    handleVoiceCommand(transcript)

                    viewModelScope.launch {
                        delay(2000)
                        _uiState.value = RemoteState.IDLE
                    }
                    return
                }

                when (response.event) {
                    EVENT_TV_LIST -> {
                        val devicesFromResponse = response.devices?.map { device ->
                            RemoteDevice(
                                id = device.id,
                                name = if (device.nickName.isNullOrEmpty()) {
                                    "TV ${device.id.takeLast(4)}"
                                } else {
                                    device.nickName
                                }
                            )
                        }?.filter { it.id != "__all__" } ?: emptyList()

                        val newDevices = if (devicesFromResponse.size > 1) {
                            listOf(RemoteDevice("__all__", "All Devices")) + devicesFromResponse
                        } else {
                            devicesFromResponse
                        }

                        _devices.value = newDevices
                        handleDefaultDeviceSelection(_devices.value)
                    }
                    else -> {
                        viewModelScope.launch {
                            if (_uiState.value is RemoteState.PROCESSING) {
                                _uiState.value = RemoteState.IDLE
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("WebSocket", "Error parsing message: ${e.message}", e)
            }
        }

        private fun handleVoiceCommand(transcript: String) {
            val command = transcript.lowercase()
            when {
                command.contains("home") -> sendRemoteAction(Icons.Rounded.Home)
                command.contains("settings") -> sendRemoteAction(Icons.Rounded.Settings)
                command.contains("volume up") || command.contains("increase volume") -> sendRemoteAction(
                    Icons.AutoMirrored.Rounded.VolumeUp
                )

                command.contains("volume down") || command.contains("decrease volume") -> sendRemoteAction(
                    Icons.AutoMirrored.Rounded.VolumeDown
                )

                command.contains("mute") -> sendRemoteAction(Icons.AutoMirrored.Rounded.VolumeOff)
                command.contains("back") -> sendRemoteAction(Icons.AutoMirrored.Rounded.ArrowBack)
                command.contains("ok") || command.contains("select") -> sendRemoteAction(Icons.Rounded.Check)
                command.contains("up") -> sendRemoteAction(Icons.Rounded.KeyboardArrowUp)
                command.contains("down") -> sendRemoteAction(Icons.Rounded.KeyboardArrowDown)
                command.contains("left") -> sendRemoteAction(Icons.AutoMirrored.Rounded.KeyboardArrowLeft)
                command.contains("right") -> sendRemoteAction(Icons.AutoMirrored.Rounded.KeyboardArrowRight)
                command.contains("schedule") -> sendRemoteAction(Icons.Rounded.Schedule)
            }
        }

        private fun handleDefaultDeviceSelection(deviceList: List<RemoteDevice>) {
            if (deviceList.isEmpty()) {
                _selectedDeviceId.value = null
                return
            }

            // Check if current selection is still valid
            val currentSelection = _selectedDeviceId.value
            val isCurrentSelectionValid = deviceList.any { it.id == currentSelection }

            if (!isCurrentSelectionValid) {
                val allDevicesOption = deviceList.find { it.id == "__all__" }
                if ((deviceList.size > 1) && (allDevicesOption != null)) {
                    // More than one device (including __all__), default to __all__
                    _selectedDeviceId.value = "__all__"
                } else {
                    // Only one device (might be __all__ or a specific one), select the first one
                    _selectedDeviceId.value = deviceList.first().id
                }
            } else if (deviceList.size == 1) {
                // List reduced to one, select it even if previous selection was something else (like __all__ which might have been removed)
                _selectedDeviceId.value = deviceList.first().id
            } else if (deviceList.size > 1 && currentSelection != "__all__") {
                // If we have multiple devices but a specific one was selected, keep it if it's still there.
                // However, the prompt says "default selection is all devices" when more than one. 
                // Let's ensure if it was a single device and now there are multiple, we don't necessarily jump to __all__ unless explicitly needed.
                // But specifically for the "list reduced to one" case:
                if (deviceList.none { it.id == "__all__" } || deviceList.size == 2 && deviceList.any { it.id == "__all__" }) {
                    // If there's only one REAL device + __all__, or just one real device:
                    val realDevices = deviceList.filter { it.id != "__all__" }
                    if (realDevices.size == 1) {
                        _selectedDeviceId.value = realDevices.first().id
                    }
                }
            }

            // Re-refining the logic based on exact prompt:
            // "When there is more than one device connected the default selection is all devices"
            // "when one of more device is disconnected resulting to the device list to one. Select the available device by default."

            val realDevices = deviceList.filter { it.id != "__all__" }
            if (realDevices.size > 1) {
                if (_selectedDeviceId.value == null || !deviceList.any { it.id == _selectedDeviceId.value }) {
                    _selectedDeviceId.value = "__all__"
                }
            } else if (realDevices.size == 1) {
                _selectedDeviceId.value = realDevices.first().id
            }
        }

        /*private fun handleDefaultDeviceSelection(deviceList: List<RemoteDevice>) {
            if (deviceList.isNotEmpty()){
                deviceList.forEach {
                    if (it.id == "__all__"){
                        selectDevice(it.id)
                    }
                }
            }
        }*/

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            _uiState.value = RemoteState.ERROR(t.message)
            Log.e("WebSocket", "Failure", t)
            if (!isManualDisconnect) {
                viewModelScope.launch {
                    delay(5000)
                    connect()
                }
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            _uiState.value = RemoteState.IDLE
            Log.d("WebSocket", "Closing: $reason")
        }
    }

    init {
        connect()
    }

    fun connect() {
        isManualDisconnect = false
        // Removed updating _statusText to "Connecting..." to keep the suggestion visible
        webSocketService.connect(
            "ws://63.178.32.34:3000/?clientType=remote&customerId=customer2",
            wsListener
        )
    }

    fun disconnect() {
        isManualDisconnect = true
        webSocketService.disconnect()
    }

    fun toggleListening(deviceId: String) {
        if (_uiState.value is RemoteState.LISTENING) {
            stopListening(deviceId)
        } else {
            startListening(deviceId)
        }
    }

    private var lastAudioTimestamp = 0L
    private val SILENCE_THRESHOLD = 500 // Threshold for 16-bit PCM
    private val SILENCE_TIMEOUT_MS = 3000L

    private fun startListening(deviceId: String) {
        webSocketService.sendEventData(SocketEventsHelper.audioStartEvent(deviceId))
        _uiState.value = RemoteState.LISTENING
        lastAudioTimestamp = System.currentTimeMillis()

        viewModelScope.launch {
            try {
                audioService.startRecording { data ->
                    webSocketService.sendAudioData(data)
                    
                    // Silence detection logic
                    if (isLoud(data)) {
                        lastAudioTimestamp = System.currentTimeMillis()
                    } else {
                        if (System.currentTimeMillis() - lastAudioTimestamp > SILENCE_TIMEOUT_MS) {
                            viewModelScope.launch {
                                stopListening(deviceId)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("Audio", "Recording failed", e)
                _uiState.value = RemoteState.ERROR("Recording failed")
            }
        }
    }

    private fun isLoud(data: ByteArray): Boolean {
        // Convert byte array to short array (16-bit PCM)
        for (i in 0 until data.size - 1 step 2) {
            val sample = ((data[i + 1].toInt() shl 8) or (data[i].toInt() and 0xFF)).toShort()
            if (Math.abs(sample.toInt()) > SILENCE_THRESHOLD) {
                return true
            }
        }
        return false
    }

    private fun stopListening(deviceId: String) {
        webSocketService.sendEventData(SocketEventsHelper.audioEndEvent(deviceId))
        audioService.stopRecording()
        _uiState.value = RemoteState.PROCESSING
    }

    fun selectDevice(id: String) {
        _selectedDeviceId.value = id
    }

    fun renameDevice(id: String, newName: String) {
        _devices.value = _devices.value.map {
            if (it.id == id) it.copy(name = newName) else it
        }
        webSocketService.sendEventData(
            SocketEventsHelper.renameDevice(
                deviceId = id,
                newName = newName
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        audioService.stopRecording()
        disconnect()
    }

    fun getExtractedMessage(result: String?): String {
        return if (result.isNullOrEmpty()){
            "Sorry, I didn’t catch that. Please try again."
        }else{
            result
        }
    }

    fun identifyDevice(deviceId: String) {
        webSocketService.sendEventData(SocketEventsHelper.identifyDeviceEvent(deviceId))
    }

    fun sendRemoteAction(icon: ImageVector?) {
        val deviceId = selectedDeviceId.value ?: return
        when (icon) {
            Icons.Rounded.Home -> {
                webSocketService.sendEventData(SocketEventsHelper.sendRemoteAction(deviceId, Home))
            }

            Icons.Rounded.Settings -> {
                webSocketService.sendEventData(
                    SocketEventsHelper.sendRemoteAction(
                        deviceId,
                        Settings
                    )
                )
            }

            Icons.AutoMirrored.Rounded.VolumeDown -> {
                webSocketService.sendEventData(
                    SocketEventsHelper.sendRemoteAction(
                        deviceId,
                        VolumeDown
                    )
                )
            }

            Icons.AutoMirrored.Rounded.VolumeUp -> {
                webSocketService.sendEventData(
                    SocketEventsHelper.sendRemoteAction(
                        deviceId,
                        VolumeUp
                    )
                )
            }

            Icons.Rounded.Schedule -> {
                webSocketService.sendEventData(
                    SocketEventsHelper.sendRemoteAction(
                        deviceId,
                        Schedule
                    )
                )
            }

            Icons.Rounded.KeyboardArrowUp -> {
                webSocketService.sendEventData(SocketEventsHelper.sendRemoteAction(deviceId, Up))
            }

            Icons.Rounded.KeyboardArrowDown -> {
                webSocketService.sendEventData(SocketEventsHelper.sendRemoteAction(deviceId, Down))
            }

            Icons.AutoMirrored.Rounded.KeyboardArrowLeft -> {
                webSocketService.sendEventData(SocketEventsHelper.sendRemoteAction(deviceId, Left))
            }

            Icons.AutoMirrored.Rounded.KeyboardArrowRight -> {
                webSocketService.sendEventData(SocketEventsHelper.sendRemoteAction(deviceId, Right))
            }

            Icons.Rounded.Check -> {
                webSocketService.sendEventData(SocketEventsHelper.sendRemoteAction(deviceId, Ok))
            }

            Icons.AutoMirrored.Rounded.ArrowBack -> {
                webSocketService.sendEventData(SocketEventsHelper.sendRemoteAction(deviceId, Back))
            }

            Icons.AutoMirrored.Rounded.VolumeOff -> {
                webSocketService.sendEventData(SocketEventsHelper.sendRemoteAction(deviceId, Mute))
            }
        }
    }
}
