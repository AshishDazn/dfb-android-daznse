package com.sample.smartremote

import com.sample.smartremote.data.RemoteDevice
import com.sample.smartremote.data.RemoteState
import com.sample.smartremote.data.SocketEventsHelper
import com.sample.smartremote.data.SocketEventsHelper.EVENT_TV_LIST
import com.sample.smartremote.data.WebSocketResponse
import dev.icerock.moko.mvvm.viewmodel.ViewModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class RemoteViewModel(private val audioService: AudioService) : ViewModel() {
    private val webSocketService = WebSocketService()

    private val _uiState = MutableStateFlow<RemoteState>(RemoteState.IDLE)
    val uiState = _uiState.asStateFlow()

    private val _statusText = MutableStateFlow(getRandomSuggestions())
    val statusText = _statusText.asStateFlow()

    private val _devices = MutableStateFlow<List<RemoteDevice>>(emptyList())
    val devices = _devices.asStateFlow()

    private val _selectedDeviceId = MutableStateFlow<String?>(null)
    val selectedDeviceId = _selectedDeviceId.asStateFlow()

    init {
        connectToWebSocket()
    }

    private fun connectToWebSocket() {
        viewModelScope.launch {
            try {
                webSocketService.connect("ws://63.178.32.34:3000/?clientType=remote&customerId=customer2")
                webSocketService.receive()
                    .onEach { text ->
                        handleWebSocketMessage(text)
                    }
                    .launchIn(viewModelScope)
            } catch (e: Exception) {
                _uiState.value = RemoteState.ERROR(e.message)
                Napier.e("WebSocket Failure", e)
                delay(5000)
                connectToWebSocket()
            }
        }
    }

    private fun handleWebSocketMessage(text: String) {
        Napier.d("WebSocket Message: $text")
        try {
            val response = Json.decodeFromString<WebSocketResponse>(text)

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
                        RemoteDevice(id = device.id, name = if (device.nickName.isNullOrEmpty()) {
                            "TV ${device.id.takeLast(4)}"
                        } else {
                            device.nickName
                        })
                    } ?: emptyList()

                    _devices.value = newDevices
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
            Napier.e("Error parsing message", e)
        }
    }

    fun toggleListening(deviceId: String?) {
        if (_uiState.value is RemoteState.LISTENING) {
            stopListening(deviceId)
        } else {
            startListening(deviceId)
        }
    }

    private fun startListening(deviceId: String?) {
        viewModelScope.launch {
            webSocketService.sendEventData(SocketEventsHelper.audioStartEvent(deviceId))
            _uiState.value = RemoteState.LISTENING

            try {
                audioService.startRecording { data ->
                    viewModelScope.launch {
                        webSocketService.sendAudioData(data)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = RemoteState.ERROR("Recording failed")
            }
        }
    }

    private fun stopListening(deviceId: String?) {
        viewModelScope.launch {
            webSocketService.sendEventData(SocketEventsHelper.audioEndEvent(deviceId))
            audioService.stopRecording()
            _uiState.value = RemoteState.PROCESSING
        }
    }

    fun selectDevice(id: String) {
        _selectedDeviceId.value = id
    }

    fun renameDevice(id: String, newName: String) {
        _devices.value = _devices.value.map {
            if (it.id == id) it.copy(name = newName) else it
        }
        viewModelScope.launch {
            webSocketService.sendEventData(SocketEventsHelper.renameDevice(deviceId = id, newName = newName))
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioService.stopRecording()
        viewModelScope.launch {
            webSocketService.disconnect()
        }
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
        val defaultMessage = "Sorry, I didn’t catch that. Please try again."
        return if (result?.transcript.isNullOrEmpty()) {
            defaultMessage
        } else {
            result.transcript
        }
    }

    fun identifyDevice(deviceId: String) {
        viewModelScope.launch {
            webSocketService.sendEventData(SocketEventsHelper.identifyDeviceEvent(deviceId))
        }
    }

    fun sendRemoteAction(action: String) {
        val deviceId = selectedDeviceId.value ?: return
        viewModelScope.launch {
            webSocketService.sendEventData(SocketEventsHelper.sendRemoteAction(deviceId, action))
        }
    }
}
