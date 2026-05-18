package com.sample.smartremote

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryRecord
import platform.AVFAudio.AVAudioTime
import platform.AVFAudio.setActive
import platform.AVFoundation.*
import platform.AudioToolbox.*
import platform.Foundation.*

actual class AudioService actual constructor() {
    private val audioEngine = AVAudioEngine()
    private var isRecording = false

    @OptIn(ExperimentalForeignApi::class)
    actual fun startRecording(onData: (ByteArray) -> Unit) {
        val audioSession = AVAudioSession.sharedInstance()
        try {
            audioSession.setCategory(AVAudioSessionCategoryRecord, error = null)
            audioSession.setActive(true, error = null)

            val inputNode = audioEngine.inputNode
            val recordingFormat = inputNode.outputFormatForBus(0u)
            
            inputNode.installTapOnBus(0u, 4096u, recordingFormat) { buffer: AVAudioPCMBuffer?, _: AVAudioTime? ->
                if (buffer != null) {
                    val frameLength = buffer.frameLength.toInt()
                    val channelData = buffer.floatChannelData
                    if (channelData != null) {
                        val data = channelData[0]
                        if (data != null) {
                            // Convert float PCM to 16-bit PCM ByteArray (Little Endian)
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

            audioEngine.prepare()
            audioEngine.startAndReturnError(null)
            isRecording = true
        } catch (e: Exception) {
            // Log error
        }
    }
    
    actual fun stopRecording() {
        if (isRecording) {
            audioEngine.inputNode.removeTapOnBus(0u)
            audioEngine.stop()
            isRecording = false
        }
    }
}
