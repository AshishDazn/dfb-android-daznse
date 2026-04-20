package com.sample.smartremote

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AudioService {
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = 4096
    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    private var noiseSuppressor: NoiseSuppressor? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var gainControl: AutomaticGainControl? = null

    @SuppressLint("MissingPermission")
    suspend fun startRecording(onBufferAvailable: (ByteArray) -> Unit) = withContext(Dispatchers.IO) {
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION, // Optimized for voice processing
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioService", "AudioRecord initialization failed")
                return@withContext
            }

            setupAudioEffects(audioRecord?.audioSessionId ?: 0)

            val buffer = ByteArray(bufferSize)
            audioRecord?.startRecording()
            isRecording = true

            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    onBufferAvailable(buffer.copyOf(read))
                }
            }
        } catch (e: Exception) {
            Log.e("AudioService", "Error during recording", e)
        } finally {
            cleanup()
        }
    }

    private fun setupAudioEffects(sessionId: Int) {
        if (sessionId == 0) return

        if (NoiseSuppressor.isAvailable()) {
            noiseSuppressor = NoiseSuppressor.create(sessionId)
            noiseSuppressor?.enabled = true
            Log.d("AudioService", "NoiseSuppressor enabled")
        }

        if (AcousticEchoCanceler.isAvailable()) {
            echoCanceler = AcousticEchoCanceler.create(sessionId)
            echoCanceler?.enabled = true
            Log.d("AudioService", "AcousticEchoCanceler enabled")
        }

        if (AutomaticGainControl.isAvailable()) {
            gainControl = AutomaticGainControl.create(sessionId)
            gainControl?.enabled = true
            Log.d("AudioService", "AutomaticGainControl enabled")
        }
    }

    fun stopRecording() {
        isRecording = false
    }

    private fun cleanup() {
        try {
            audioRecord?.apply {
                if (state == AudioRecord.STATE_INITIALIZED) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioService", "Error cleaning up AudioRecord", e)
        }
        audioRecord = null

        noiseSuppressor?.release()
        echoCanceler?.release()
        gainControl?.release()

        noiseSuppressor = null
        echoCanceler = null
        gainControl = null
    }
}
