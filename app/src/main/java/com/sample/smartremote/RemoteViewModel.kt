package com.sample.smartremote

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sample.smartremote.data.RemoteState
import com.sample.smartremote.data.SocketEventsHelper
import com.sample.smartremote.data.repository.RemoteRepository
import com.sample.smartremote.logic.ActionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.vector.ImageVector

class RemoteViewModel(
    private val remoteRepository: RemoteRepository,
    private val actionHandler: ActionHandler
) : ViewModel() {

    private val audioService = AudioService()

    private val _uiState = MutableStateFlow<RemoteState>(RemoteState.IDLE)
    val uiState = _uiState.asStateFlow()

    val devices = remoteRepository.devices
    val selectedDeviceId = remoteRepository.selectedDeviceId

    init {
        viewModelScope.launch {
            remoteRepository.onTranscriptReceived.collect { transcript ->
                if (transcript != null) {
                    val message = getExtractedMessage(transcript)
                    _uiState.value = RemoteState.RESULT(message)
                    handleVoiceCommand(message)

                    delay(2000)
                    _uiState.value = RemoteState.IDLE
                    remoteRepository.clearTranscript()
                }
            }
        }
    }

    fun connect() {
        remoteRepository.connect()
    }

    fun disconnect() {
        remoteRepository.disconnect()
    }

    fun toggleListening(deviceId: String) {
        if (_uiState.value is RemoteState.LISTENING) {
            stopListening(deviceId)
        } else {
            startListening(deviceId)
        }
    }

    private fun startListening(deviceId: String) {
        remoteRepository.sendEvent(SocketEventsHelper.audioStartEvent(deviceId))
        _uiState.value = RemoteState.LISTENING

        viewModelScope.launch {
            try {
                audioService.startRecording { data ->
                    remoteRepository.sendAudioData(data)
                }
            } catch (e: Exception) {
                Log.e("Audio", "Recording failed", e)
                _uiState.value = RemoteState.ERROR("Recording failed")
            }
        }
    }

    private fun stopListening(deviceId: String) {
        remoteRepository.sendEvent(SocketEventsHelper.audioEndEvent(deviceId))
        audioService.stopRecording()
        _uiState.value = RemoteState.PROCESSING
    }

    fun selectDevice(id: String) {
        remoteRepository.selectDevice(id)
    }

    fun renameDevice(id: String, newName: String) {
        remoteRepository.renameDeviceLocal(id, newName)
        remoteRepository.sendEvent(
            SocketEventsHelper.renameDevice(
                deviceId = id,
                newName = newName
            )
        )
    }

    fun identifyDevice(deviceId: String) {
        remoteRepository.sendEvent(SocketEventsHelper.identifyDeviceEvent(deviceId))
    }

    fun sendRemoteAction(icon: ImageVector?) {
        val deviceId = selectedDeviceId.value ?: return
        val command = actionHandler.mapIconToCommand(icon)
        if (command != null) {
            remoteRepository.sendEvent(SocketEventsHelper.sendRemoteAction(deviceId, command))
        }
    }

    private fun handleVoiceCommand(transcript: String) {
        val icon = actionHandler.mapVoiceCommandToIcon(transcript)
        if (icon != null) {
            sendRemoteAction(icon)
        }
    }

    private fun getExtractedMessage(result: String?): String {
        return if (result.isNullOrEmpty()) {
            "Sorry, I didn’t catch that. Please try again."
        } else {
            result
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioService.stopRecording()
        disconnect()
    }
}
