package com.sample.smartremote

import androidx.compose.ui.graphics.vector.ImageVector
import com.sample.smartremote.data.*
import com.sample.smartremote.data.SocketEventsHelper.EVENT_TV_LIST
import com.sample.smartremote.data.repository.AuthRepository
import com.sample.smartremote.data.repository.RemoteRepository
import com.sample.smartremote.logic.ActionHandler
import dev.icerock.moko.mvvm.viewmodel.ViewModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RemoteViewModel(
    private val remoteRepository: RemoteRepository,
    private val authRepository: AuthRepository,
    private val actionHandler: ActionHandler,
    private val audioService: AudioService
) : ViewModel() {

    private val _uiState = MutableStateFlow<RemoteState>(RemoteState.IDLE)
    val uiState = _uiState.asStateFlow()

    private val _statusText = MutableStateFlow(getRandomSuggestions())
    val statusText = _statusText.asStateFlow()

    val devices = remoteRepository.devices
    val selectedDeviceId = remoteRepository.selectedDeviceId

    private val _isAuthorized = MutableStateFlow(authRepository.isAuthorized())
    val isAuthorized = _isAuthorized.asStateFlow()

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn = _isLoggingIn.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError = _loginError.asStateFlow()

    init {
        if (_isAuthorized.value) {
            connect()
        }
        observeTranscripts()
    }

    private fun observeTranscripts() {
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
        remoteRepository.connect(viewModelScope)
    }

    fun disconnect() {
        viewModelScope.launch {
            remoteRepository.disconnect()
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _isLoggingIn.value = true
            _loginError.value = null
            val result = authRepository.signIn(email, password)
            result.onSuccess {
                _isAuthorized.value = true
                connect()
            }
            result.onFailure { error ->
                _loginError.value = error.message ?: "Unknown error"
                Napier.e("Login error", error)
            }
            _isLoggingIn.value = false
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
            remoteRepository.sendEvent(SocketEventsHelper.audioStartEvent(deviceId))
            _uiState.value = RemoteState.LISTENING

            try {
                audioService.startRecording { data ->
                    viewModelScope.launch {
                        remoteRepository.sendAudioData(data)
                    }
                }
            } catch (e: Exception) {
                Napier.e("Recording failed", e)
                _uiState.value = RemoteState.ERROR("Recording failed")
            }
        }
    }

    private fun stopListening(deviceId: String?) {
        viewModelScope.launch {
            remoteRepository.sendEvent(SocketEventsHelper.audioEndEvent(deviceId))
            audioService.stopRecording()
            _uiState.value = RemoteState.PROCESSING
        }
    }

    fun selectDevice(id: String) {
        remoteRepository.selectDevice(id)
    }

    fun renameDevice(id: String, newName: String) {
        remoteRepository.renameDeviceLocal(id, newName)
        viewModelScope.launch {
            remoteRepository.sendEvent(SocketEventsHelper.renameDevice(deviceId = id, newName = newName))
        }
    }

    fun identifyDevice(deviceId: String) {
        viewModelScope.launch {
            remoteRepository.sendEvent(SocketEventsHelper.identifyDeviceEvent(deviceId))
        }
    }

    fun sendRemoteAction(action: String) {
        val deviceId = selectedDeviceId.value ?: return
        val command = when(action) {
            "Home" -> SocketEventsHelper.Home
            "Up" -> SocketEventsHelper.Up
            "Down" -> SocketEventsHelper.Down
            "Left" -> SocketEventsHelper.Left
            "Right" -> SocketEventsHelper.Right
            "Ok" -> SocketEventsHelper.Ok
            "Back" -> SocketEventsHelper.Back
            "Mute" -> SocketEventsHelper.Mute
            "VolumeUp" -> SocketEventsHelper.VolumeUp
            "VolumeDown" -> SocketEventsHelper.VolumeDown
            "Schedule" -> SocketEventsHelper.Schedule
            "Settings" -> SocketEventsHelper.Settings
            else -> action
        }
        viewModelScope.launch {
            remoteRepository.sendEvent(SocketEventsHelper.sendRemoteAction(deviceId, command))
        }
    }

    private fun sendRemoteActionByIcon(icon: ImageVector?) {
        val deviceId = selectedDeviceId.value ?: return
        val command = actionHandler.mapIconToCommand(icon)
        if (command != null) {
            viewModelScope.launch {
                remoteRepository.sendEvent(SocketEventsHelper.sendRemoteAction(deviceId, command))
            }
        }
    }

    private fun handleVoiceCommand(transcript: String) {
        val icon = actionHandler.mapVoiceCommandToIcon(transcript)
        if (icon != null) {
            sendRemoteActionByIcon(icon)
        }
    }

    private fun getExtractedMessage(transcript: String?): String {
        return if (transcript.isNullOrEmpty()) {
            "Sorry, I didn’t catch that. Please try again."
        } else {
            transcript
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

    override fun onCleared() {
        super.onCleared()
        audioService.stopRecording()
        disconnect()
    }
}
