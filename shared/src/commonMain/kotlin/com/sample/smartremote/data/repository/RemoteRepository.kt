package com.sample.smartremote.data.repository

import com.sample.smartremote.WebSocketService
import com.sample.smartremote.data.Config
import com.sample.smartremote.data.RemoteDevice
import com.sample.smartremote.data.SocketEventsHelper
import com.sample.smartremote.data.WebSocketResponse
import io.github.aakira.napier.Napier
import io.ktor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class RemoteRepository(
    private val webSocketService: WebSocketService,
    private val authRepository: AuthRepository
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val _devices = MutableStateFlow<List<RemoteDevice>>(emptyList())
    val devices = _devices.asStateFlow()

    private val _selectedDeviceId = MutableStateFlow<String?>(null)
    val selectedDeviceId = _selectedDeviceId.asStateFlow()

    private val _onTranscriptReceived = MutableStateFlow<String?>(null)
    val onTranscriptReceived = _onTranscriptReceived.asStateFlow()

    private var isManualDisconnect = false

    fun connect(scope: CoroutineScope) {
        isManualDisconnect = false
        val viewerId = getViewerIdFromToken() ?: Config.DEFAULT_VIEWER_ID
        scope.launch {
            try {
                val wsUrl = Config.WS_URL
                webSocketService.connect("${wsUrl}?clientType=remote&customerId=$viewerId")
                webSocketService.receive().collect { text ->
                    handleMessage(text)
                }
            } catch (e: Exception) {
                if (!isManualDisconnect) {
                    Napier.e(message = "[${Config.LOG_TAG}] WebSocket connection failed: ${e.message}", throwable = e, tag = Config.LOG_TAG)
                } else {
                    Napier.d(message = "[${Config.LOG_TAG}] WebSocket disconnected manually", tag = Config.LOG_TAG)
                }
            }
        }
    }

    private fun handleMessage(text: String) {
        Napier.d(message = "[${Config.LOG_TAG}] WebSocket Response: $text", tag = Config.LOG_TAG)
        try {
            val response = json.decodeFromString<WebSocketResponse>(text)
            
            if (response.type == "final_transcript") {
                _onTranscriptReceived.value = response.transcript
                return
            }

            if (response.event == SocketEventsHelper.EVENT_TV_LIST) {
                handleDeviceList(response)
            }
        } catch (e: Exception) {
            Napier.e(message = "[${Config.LOG_TAG}] Error parsing message: ${e.message}", throwable = e, tag = Config.LOG_TAG)
        }
    }

    private fun getViewerIdFromToken(): String? {
        val token = authRepository.getAuthToken() ?: return null
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null
            val payload = parts[1]
            val decodedBytes = payload.decodeBase64Bytes()
            val decodedString = decodedBytes.decodeToString()
            val jsonObject = Json.parseToJsonElement(decodedString).jsonObject
            jsonObject["viewerId"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            Napier.e(message = "[${Config.LOG_TAG}] Error decoding token: ${e.message}", tag = Config.LOG_TAG)
            null
        }
    }

    suspend fun disconnect() {
        isManualDisconnect = true
        webSocketService.disconnect()
    }

    suspend fun sendAudioData(data: ByteArray) {
        webSocketService.sendAudioData(data)
    }

    suspend fun sendEvent(eventJson: String) {
        webSocketService.sendEventData(eventJson)
    }

    fun selectDevice(id: String) {
        _selectedDeviceId.value = id
    }

    fun clearTranscript() {
        _onTranscriptReceived.value = null
    }

    private fun handleDeviceList(response: WebSocketResponse) {
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
        handleDefaultDeviceSelection(newDevices)
    }

    private fun handleDefaultDeviceSelection(deviceList: List<RemoteDevice>) {
        val realDevices = deviceList.filter { it.id != "__all__" }
        if (realDevices.size > 1) {
            if (_selectedDeviceId.value == null || !deviceList.any { it.id == _selectedDeviceId.value }) {
                _selectedDeviceId.value = "__all__"
            }
        } else if (realDevices.size == 1) {
            _selectedDeviceId.value = realDevices.first().id
        } else if (realDevices.isEmpty()) {
            _selectedDeviceId.value = null
        }
    }

    fun renameDeviceLocal(id: String, newName: String) {
        _devices.value = _devices.value.map {
            if (it.id == id) it.copy(name = newName) else it
        }
    }
}
