package com.sample.smartremote

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.sample.smartremote.data.RemoteDevice
import com.sample.smartremote.data.RemoteState
import com.sample.smartremote.data.SocketEventsHelper
import com.sample.smartremote.data.SocketEventsHelper.EVENT_TV_LIST
import com.sample.smartremote.data.SocketEventsHelper.Home
import com.sample.smartremote.data.SocketEventsHelper.Schedule
import com.sample.smartremote.data.SocketEventsHelper.Settings
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

    private val _statusText = MutableStateFlow(getRandomSuggestions())
    val statusText = _statusText.asStateFlow()

    private val _devices = MutableStateFlow<List<RemoteDevice>>(emptyList())
    val devices = _devices.asStateFlow()

    private val _selectedDeviceId = MutableStateFlow<String?>(null)
    val selectedDeviceId = _selectedDeviceId.asStateFlow()

    private val wsListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d("WebSocket", "Connected")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d("WebSocket", "Message: $text")
            try {
                val response = gson.fromJson(text, WebSocketResponse::class.java)
                
                if (response.type == "final_transcript") {
                    val transcript = response.transcript ?: ""
                    _uiState.value = RemoteState.RESULT(transcript)
                    
                    viewModelScope.launch {
                        delay(2000)
                        _uiState.value = RemoteState.IDLE
                    }
                    return
                }

                when (response.event) {
                    EVENT_TV_LIST -> {
                        val newDevices = response.devices?.map { device ->
                            RemoteDevice(id = device.id, name = if (device.nickName.isNullOrEmpty()){
                                "TV ${device.id.takeLast(4)}"
                            }else{
                                device.nickName
                            })
                        } ?: emptyList()

                        _devices.value = newDevices
                        _devices.value.forEach {
                            if (it.id == "__all__"){
                                selectDevice(it.id)
                            }
                        }
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
                Log.e("WebSocket", "Error parsing message", e)
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            _uiState.value = RemoteState.ERROR(t.message)
            Log.e("WebSocket", "Failure", t)
            viewModelScope.launch {
                delay(5000)
                connectToWebSocket()
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            _uiState.value = RemoteState.IDLE
            Log.d("WebSocket", "Closing: $reason")
        }
    }

    init {
        connectToWebSocket()
    }

    private fun connectToWebSocket() {
        // Removed updating _statusText to "Connecting..." to keep the suggestion visible
        webSocketService.connect("ws://63.178.32.34:3000/?clientType=remote&customerId=customer2", wsListener)
    }

    fun toggleListening(deviceId: String?) {
        if (_uiState.value is RemoteState.LISTENING) {
            stopListening(deviceId)
        } else {
            startListening(deviceId)
        }
    }

    private fun startListening(deviceId: String?) {
        webSocketService.sendEventData(SocketEventsHelper.audioStartEvent(deviceId))
        _uiState.value = RemoteState.LISTENING

        viewModelScope.launch {
            try {
                audioService.startRecording { data ->
                    webSocketService.sendAudioData(data)
                }
            } catch (e: Exception) {
                _uiState.value = RemoteState.ERROR("Recording failed")
            }
        }
    }

    private fun stopListening(deviceId: String?) {
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
        webSocketService.sendEventData(SocketEventsHelper.renameDevice(deviceId = id, newName = newName))
    }

    override fun onCleared() {
        super.onCleared()
        audioService.stopRecording()
        webSocketService.disconnect()
    }

    fun getRandomSuggestions(): String {
        val listOfCommands = listOf(
            "\'Play Red Bull TV\'",
            "\'Go to Schedule\'",
            "\'Play Playlist\'",
            "\'Show me upcoming Bundesliga Matches\'"
        )
        return listOfCommands.random()
    }

    fun getExtractedMessage(result: RemoteState.RESULT?): String {
        val defaultMessage =  "Sorry, I didn’t catch that. Please try again."
        return if (result?.transcript.isNullOrEmpty()){
            defaultMessage
        }else{
            result.transcript
        }
    }

    fun identifyDevice(deviceId: String) {
        webSocketService.sendEventData(SocketEventsHelper.identifyDeviceEvent(deviceId))
    }

    fun sendRemoteAction(icon: ImageVector) {
        val deviceId = selectedDeviceId.value ?: return
        when(icon){
            Icons.Rounded.Home-> {
                webSocketService.sendEventData(SocketEventsHelper.sendRemoteAction(deviceId ,Home))
            }
            Icons.Rounded.Settings-> {
                webSocketService.sendEventData(SocketEventsHelper.sendRemoteAction(deviceId,Settings))
            }
            Icons.AutoMirrored.Rounded.VolumeDown-> {
                webSocketService.sendEventData(SocketEventsHelper.sendRemoteAction(deviceId,VolumeDown))
            }
            Icons.AutoMirrored.Rounded.VolumeUp-> {
                webSocketService.sendEventData(SocketEventsHelper.sendRemoteAction(deviceId,VolumeUp))
            }
            Icons.Rounded.Schedule-> {
                webSocketService.sendEventData(SocketEventsHelper.sendRemoteAction(deviceId,Schedule))
            }

        }
    }
}
