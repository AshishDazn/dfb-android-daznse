package com.sample.smartremote

import com.sample.smartremote.data.Config
import io.github.aakira.napier.Napier
import platform.Speech.*
import platform.Foundation.*
import platform.AVFAudio.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.NSObjectProtocol
import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
actual class SpeechToTextService {
    private var speechRecognizer: SFSpeechRecognizer? = null
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest? = null
    private var recognitionTask: SFSpeechRecognitionTask? = null
    private val audioEngine = AVAudioEngine()
    private var configChangeObserver: NSObjectProtocol? = null
    private var isStarted = false
    private var hasTap = false

    actual fun isAvailable(): Boolean {
        return SFSpeechRecognizer.supportedLocales().isNotEmpty()
    }

    init {
        setupConfigChangeObserver()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun setupConfigChangeObserver() {
        configChangeObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVAudioEngineConfigurationChangeNotification,
            `object` = audioEngine,
            queue = NSOperationQueue.mainQueue,
            usingBlock = { _ ->
                Napier.d(message = "[${Config.LOG_TAG}] STT iOS AudioEngine configuration change detected", tag = Config.LOG_TAG)
                if (recognitionRequest != null && !audioEngine.running) {
                    try {
                        audioEngine.prepare()
                        audioEngine.startAndReturnError(null)
                        Napier.d(message = "[${Config.LOG_TAG}] STT iOS AudioEngine restarted after config change", tag = Config.LOG_TAG)
                    } catch (e: Exception) {
                        Napier.e(message = "[${Config.LOG_TAG}] STT Failed to restart iOS AudioEngine", throwable = e, tag = Config.LOG_TAG)
                    }
                }
            }
        )
    }

    actual fun startListening(onResult: (String, Boolean) -> Unit, onError: (String) -> Unit) {
        val locale = NSLocale.currentLocale
        speechRecognizer = SFSpeechRecognizer(locale)

        SFSpeechRecognizer.requestAuthorization { status ->
            dispatch_async(dispatch_get_main_queue()) {
                if (status == SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized) {
                    try {
                        startRecording(onResult, onError)
                    } catch (e: Exception) {
                        onError("Failed to start recording: ${e.message}")
                    }
                } else {
                    onError("Speech recognition not authorized")
                }
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun startRecording(onResult: (String, Boolean) -> Unit, onError: (String) -> Unit) {
        if (isStarted || hasTap) {
            stopListening()
        }

        val audioSession = AVAudioSession.sharedInstance()
        try {
            audioSession.setCategory(AVAudioSessionCategoryPlayAndRecord, withOptions = AVAudioSessionCategoryOptionDefaultToSpeaker, error = null)
            audioSession.setMode(AVAudioSessionModeSpokenAudio, error = null)
            audioSession.setActive(true, error = null)
        } catch (e: Exception) {
            Napier.e(message = "[${Config.LOG_TAG}] STT Failed to set up AudioSession", throwable = e, tag = Config.LOG_TAG)
        }

        recognitionRequest = SFSpeechAudioBufferRecognitionRequest().apply {
            shouldReportPartialResults = true
            requiresOnDeviceRecognition = true
        }

        val inputNode = audioEngine.inputNode
        val recordingFormat = inputNode.outputFormatForBus(0u)
        
        if (recordingFormat.sampleRate == 0.0) {
            Napier.w(message = "[${Config.LOG_TAG}] STT iOS AudioEngine inputNode sampleRate is 0.0, possible reconfig pending", tag = Config.LOG_TAG)
        }

        inputNode.removeTapOnBus(0u)
        inputNode.installTapOnBus(0u, 4096u, recordingFormat) { buffer, _ ->
            autoreleasepool {
                if (buffer != null && isStarted) {
                    recognitionRequest?.appendAudioPCMBuffer(buffer)
                }
            }
        }
        hasTap = true

        audioEngine.prepare()
        try {
            val started = memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                val success = audioEngine.startAndReturnError(error.ptr)
                if (!success) {
                    onError("Could not start audio engine: ${error.value?.localizedDescription}")
                }
                success
            }
            if (!started) return
            isStarted = true
        } catch (e: Exception) {
            onError("Could not start audio engine: ${e.message}")
            return
        }

        recognitionTask = speechRecognizer?.recognitionTaskWithRequest(recognitionRequest!!) { result, error ->
            dispatch_async(dispatch_get_main_queue()) {
                if (result != null) {
                    val transcript = result.bestTranscription.formattedString
                    if (result.isFinal()) {
                        onResult(transcript, true)
                        cleanupResources()
                    } else {
                        onResult(transcript, false)
                    }
                }
                if (error != null) {
                    // Ignore "User canceled" error when stopListening is called
                    if (error.code != 1107L && error.code != 301L) {
                        onError(error.localizedDescription)
                    }
                    cleanupResources()
                }
            }
        }
    }

    actual fun stopListening() {
        if (isStarted || hasTap) {
            if (audioEngine.running) {
                audioEngine.stop()
            }
            if (hasTap) {
                audioEngine.inputNode.removeTapOnBus(0u)
                hasTap = false
            }
            
            try {
                AVAudioSession.sharedInstance().setActive(false, withOptions = AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation, error = null)
            } catch (e: Exception) {
                Napier.e(message = "[${Config.LOG_TAG}] STT Failed to deactivate AudioSession", tag = Config.LOG_TAG)
            }

            recognitionRequest?.endAudio()
            isStarted = false
        }
    }

    private fun cleanupResources() {
        if (isStarted || hasTap) {
            if (audioEngine.running) {
                audioEngine.stop()
            }
            if (hasTap) {
                audioEngine.inputNode.removeTapOnBus(0u)
                hasTap = false
            }
            isStarted = false
        }

        recognitionRequest = null
        recognitionTask = null
    }
}
