package com.sample.smartremote

import com.sample.smartremote.data.Config
import io.github.aakira.napier.Napier
import kotlinx.cinterop.*
import platform.AVFAudio.*
import platform.Foundation.NSError
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.darwin.NSObjectProtocol

actual class AudioService actual constructor() {
    private val audioEngine = AVAudioEngine()
    private var isRecording = false
    private var configChangeObserver: NSObjectProtocol? = null
    private var hasTap = false
    private var isStarted = false

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
                Napier.d(message = "[${Config.LOG_TAG}] iOS AudioEngine configuration change detected", tag = Config.LOG_TAG)
                if (isRecording && !audioEngine.running) {
                    try {
                        audioEngine.prepare()
                        audioEngine.startAndReturnError(null)
                        Napier.d(message = "[${Config.LOG_TAG}] iOS AudioEngine restarted after config change", tag = Config.LOG_TAG)
                    } catch (e: Exception) {
                        Napier.e(message = "[${Config.LOG_TAG}] Failed to restart iOS AudioEngine", throwable = e, tag = Config.LOG_TAG)
                    }
                }
            }
        )
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    actual fun startRecording(onData: (ByteArray) -> Unit) {
        if (audioEngine.running) {
            stopRecording()
        }

        val audioSession = AVAudioSession.sharedInstance()
        try {
            audioSession.setCategory(AVAudioSessionCategoryPlayAndRecord, withOptions = AVAudioSessionCategoryOptionDefaultToSpeaker, error = null)
            audioSession.setMode(AVAudioSessionModeSpokenAudio, error = null)
            audioSession.setActive(true, error = null)

            val inputNode = audioEngine.inputNode
            val recordingFormat = inputNode.outputFormatForBus(0u)
            
            if (recordingFormat.sampleRate == 0.0) {
                Napier.w(message = "[${Config.LOG_TAG}] iOS AudioEngine inputNode sampleRate is 0.0, possible reconfig pending", tag = Config.LOG_TAG)
            }

            inputNode.removeTapOnBus(0u)
            inputNode.installTapOnBus(0u, 4096u, recordingFormat) { buffer: AVAudioPCMBuffer?, _: AVAudioTime? ->
                autoreleasepool {
                    if (buffer != null) {
                        val frameLength = buffer.frameLength.toInt()
                        val channelData = buffer.floatChannelData
                        if (channelData != null) {
                            val data = channelData[0]
                            if (data != null) {
                                val byteArray = ByteArray(frameLength * 2)
                                for (i in 0 until frameLength) {
                                    val sample = (data[i] * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
                                    byteArray[i * 2] = (sample.toInt() and 0xFF).toByte()
                                    byteArray[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
                                }
                                onData(byteArray)
                            }
                        }
                    }
                }
            }
            hasTap = true

            audioEngine.prepare()
            val error = memScoped {
                alloc<ObjCObjectVar<NSError?>>()
            }
            if (!audioEngine.startAndReturnError(error.ptr)) {
                Napier.e(message = "[${Config.LOG_TAG}] iOS AudioEngine start failed: ${error.value?.localizedDescription}", tag = Config.LOG_TAG)
            } else {
                isRecording = true
                isStarted = true
                Napier.d(message = "[${Config.LOG_TAG}] iOS AudioEngine started", tag = Config.LOG_TAG)
            }
        } catch (e: Exception) {
            Napier.e(message = "[${Config.LOG_TAG}] iOS Recording failed", throwable = e, tag = Config.LOG_TAG)
        }
    }
    
    @OptIn(ExperimentalForeignApi::class)
    actual fun stopRecording() {
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
                Napier.e(message = "[${Config.LOG_TAG}] Failed to deactivate AudioSession", throwable = e, tag = Config.LOG_TAG)
            }

            isRecording = false
            isStarted = false
        }
    }

    fun cleanup() {
        configChangeObserver?.let {
            NSNotificationCenter.defaultCenter.removeObserver(it)
        }
    }
}
