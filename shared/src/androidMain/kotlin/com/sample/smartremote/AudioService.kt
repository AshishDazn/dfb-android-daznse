package com.sample.smartremote

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import com.sample.smartremote.data.Config
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

actual class AudioService {
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = 4096
    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    private var noiseSuppressor: NoiseSuppressor? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var gainControl: AutomaticGainControl? = null

    private val recordingScope = CoroutineScope(Dispatchers.IO)

    @SuppressLint("MissingPermission")
    actual fun startRecording(onData: (ByteArray) -> Unit) {
        recordingScope.launch {
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Napier.e(message = "[${Config.LOG_TAG}] AudioRecord initialization failed", tag = Config.LOG_TAG)
                    return@launch
                }

                setupAudioEffects(audioRecord?.audioSessionId ?: 0)

                val buffer = ByteArray(bufferSize)
                audioRecord?.startRecording()
                isRecording = true

                while (isRecording) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        onData(buffer.copyOf(read))
                    }
                }
            } catch (e: Exception) {
                Napier.e(message = "[${Config.LOG_TAG}] Error during recording", throwable = e, tag = Config.LOG_TAG)
            } finally {
                cleanup()
            }
        }
    }

    private fun setupAudioEffects(sessionId: Int) {
        if (sessionId == 0) return

        if (NoiseSuppressor.isAvailable()) {
            noiseSuppressor = NoiseSuppressor.create(sessionId)
            noiseSuppressor?.enabled = true
            Napier.d(message = "[${Config.LOG_TAG}] NoiseSuppressor enabled", tag = Config.LOG_TAG)
        }

        if (AcousticEchoCanceler.isAvailable()) {
            echoCanceler = AcousticEchoCanceler.create(sessionId)
            echoCanceler?.enabled = true
            Napier.d(message = "[${Config.LOG_TAG}] AcousticEchoCanceler enabled", tag = Config.LOG_TAG)
        }

        if (AutomaticGainControl.isAvailable()) {
            gainControl = AutomaticGainControl.create(sessionId)
            gainControl?.enabled = true
            Napier.d(message = "[${Config.LOG_TAG}] AutomaticGainControl enabled", tag = Config.LOG_TAG)
        }
    }

    actual fun stopRecording() {
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
            Napier.e(message = "[${Config.LOG_TAG}] Error cleaning up AudioRecord", throwable = e, tag = Config.LOG_TAG)
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
