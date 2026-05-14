package com.sample.smartremote.data.repository

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.sample.smartremote.BuildConfig
import com.sample.smartremote.WebSocketService
import com.sample.smartremote.data.RemoteDevice
import com.sample.smartremote.data.SocketEventsHelper
import com.sample.smartremote.data.WebSocketResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.nio.charset.Charset

class RemoteRepository(
    private val webSocketService: WebSocketService,
    private val authRepository: AuthRepository,
    private val gson: Gson
) {
    private val _devices = MutableStateFlow<List<RemoteDevice>>(emptyList())
    val devices = _devices.asStateFlow()

    private val _selectedDeviceId = MutableStateFlow<String?>(null)
    val selectedDeviceId = _selectedDeviceId.asStateFlow()

    private val _onTranscriptReceived = MutableStateFlow<String?>(null)
    val onTranscriptReceived = _onTranscriptReceived.asStateFlow()

    private var isManualDisconnect = false

    private val wsListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d("WebSocket:", "Connected")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d("WebSocket:", "Message: $text")
            try {
                val response = gson.fromJson(text, WebSocketResponse::class.java)
                
                if (response.type == "final_transcript") {
                    _onTranscriptReceived.value = response.transcript
                    return
                }

                if (response.event == SocketEventsHelper.EVENT_TV_LIST) {
                    handleDeviceList(response)
                }
            } catch (e: Exception) {
                Log.d("WebSocket:", "Error parsing message: ${e.message}", e)
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.d("WebSocket:", "Failure", t)
            // Reconnection logic could be here or in ViewModel
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
            Log.d("WebSocket:", "reason")
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosing(webSocket, code, reason)
            Log.d("WebSocket:", "reason")
        }
    }

    fun connect() {
        isManualDisconnect = false
        val viewerId = getViewerIdFromToken() ?: "customer2"
        webSocketService.connect(
            "${BuildConfig.WS_URL}?clientType=remote&customerId=$viewerId",
            wsListener
        )
    }

    private fun getViewerIdFromToken(): String? {
        val token = authRepository.getAuthToken() ?: return null
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null
            val payload = parts[1]
            val decodedBytes = Base64.decode(payload, Base64.URL_SAFE)
            val decodedString = String(decodedBytes, Charset.forName("UTF-8"))
            val jsonObject = gson.fromJson(decodedString, JsonObject::class.java)
            jsonObject.get("viewerId")?.asString
        } catch (e: Exception) {
            Log.e("RemoteRepository", "Error decoding token: ${e.message}")
            null
        }
    }

    fun disconnect() {
        isManualDisconnect = true
        webSocketService.disconnect()
    }

    fun sendAudioData(data: ByteArray) {
        webSocketService.sendAudioData(data)
    }

    fun sendEvent(eventJson: String) {
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
