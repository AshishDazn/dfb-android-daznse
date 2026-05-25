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
    private var sampleCounter = 0.0
    private val pcmBuffer = ShortArray(2048)
    private var pcmBufferIndex = 0

    actual fun isAvailable(): Boolean {
        return true // iOS audio recording is generally available
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
                Napier.d(message = "[${Config.LOG_TAG}] iOS AudioService configuration change detected", tag = Config.LOG_TAG)
                if (isRecording && !audioEngine.running) {
                    try {
                        audioEngine.prepare()
                        audioEngine.startAndReturnError(null)
                        Napier.d(message = "[${Config.LOG_TAG}] iOS AudioService restarted after config change", tag = Config.LOG_TAG)
                    } catch (e: Exception) {
                        Napier.e(message = "[${Config.LOG_TAG}] Failed to restart iOS AudioService", throwable = e, tag = Config.LOG_TAG)
                    }
                }
            }
        )
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    actual fun startRecording(onData: (ByteArray) -> Unit) {
        if (isStarted || hasTap) {
            stopRecording()
        }

        val audioSession = AVAudioSession.sharedInstance()
        try {
            audioSession.setCategory(AVAudioSessionCategoryPlayAndRecord, withOptions = AVAudioSessionCategoryOptionDefaultToSpeaker, error = null)
            audioSession.setMode(AVAudioSessionModeSpokenAudio, error = null)
            audioSession.setPreferredSampleRate(16000.0, error = null)
            audioSession.setActive(true, error = null)

            val inputNode = audioEngine.inputNode
            val nativeFormat = inputNode.outputFormatForBus(0u)
            val nativeSampleRate = nativeFormat.sampleRate
            
            if (nativeSampleRate == 0.0) {
                Napier.w(message = "[${Config.LOG_TAG}] iOS AudioService inputNode sampleRate is 0.0", tag = Config.LOG_TAG)
            }

            val ratio = if (nativeSampleRate > 0) nativeSampleRate / 16000.0 else 1.0
            sampleCounter = 0.0
            pcmBufferIndex = 0

            inputNode.installTapOnBus(0u, 4096u, nativeFormat) { buffer: AVAudioPCMBuffer?, _: AVAudioTime? ->
                autoreleasepool {
                    if (buffer != null && isRecording) {
                        val frameLength = buffer.frameLength.toInt()
                        val channelData = buffer.floatChannelData
                        if (channelData != null) {
                            val data = channelData[0]
                            if (data != null) {
                                for (i in 0 until frameLength) {
                                    sampleCounter += 1.0
                                    if (sampleCounter >= ratio) {
                                        val sample = (data[i] * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
                                        pcmBuffer[pcmBufferIndex++] = sample
                                        sampleCounter -= ratio
                                        
                                        if (pcmBufferIndex == 2048) {
                                            val byteArray = ByteArray(4096)
                                            for (j in 0 until 2048) {
                                                val s = pcmBuffer[j]
                                                byteArray[j * 2] = (s.toInt() and 0xFF).toByte()
                                                byteArray[j * 2 + 1] = ((s.toInt() shr 8) and 0xFF).toByte()
                                            }
                                            onData(byteArray)
                                            pcmBufferIndex = 0
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            hasTap = true

            audioEngine.prepare()
            val started = memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                val success = audioEngine.startAndReturnError(error.ptr)
                if (!success) {
                    Napier.e(message = "[${Config.LOG_TAG}] iOS AudioService start failed: ${error.value?.localizedDescription}", tag = Config.LOG_TAG)
                }
                success
            }
            
            if (!started) {
                stopRecording()
            } else {
                isRecording = true
                isStarted = true
                Napier.d(message = "[${Config.LOG_TAG}] iOS AudioService started", tag = Config.LOG_TAG)
            }
        } catch (e: Exception) {
            Napier.e(message = "[${Config.LOG_TAG}] iOS AudioService Recording failed", throwable = e, tag = Config.LOG_TAG)
            stopRecording()
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
