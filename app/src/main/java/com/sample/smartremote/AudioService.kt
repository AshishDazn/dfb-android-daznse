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

    // Noise Suppression State
    private var noiseFloor = 0.0
    private var calibrationSamples = 0
    private val CALIBRATION_COUNT = 20 // ~500ms at 4096 buffer size / 16kHz
    private var prevSample = 0.toShort()

    // HPF State (120Hz cutoff at 16kHz)
    private var lastX = 0.0
    private var lastY = 0.0
    private val HPF_ALPHA = 0.955

    // AGC State
    private var currentGain = 1.0
    private val TARGET_RMS = 2500.0 // Target amplitude for normalized speech
    private val MAX_GAIN = 8.0     // Max boost for distant users
    private val GAIN_SMOOTHING = 0.02 // Slow attack/release to prevent pumping

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

            val buffer = ShortArray(bufferSize / 2)
            audioRecord?.startRecording()
            isRecording = true

            // Reset suppression state
            calibrationSamples = 0
            noiseFloor = 0.0
            prevSample = 0
            lastX = 0.0
            lastY = 0.0
            currentGain = 1.0

            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    val processedBuffer = processAudio(buffer.copyOf(read))
                    onBufferAvailable(shortArrayToByteArray(processedBuffer))
                }
            }
        } catch (e: Exception) {
            Log.e("AudioService", "Error during recording", e)
        } finally {
            cleanup()
        }
    }

    private fun processAudio(buffer: ShortArray): ShortArray {
        val inputRms = calculateRMS(buffer)
        
        // 1. Adaptive Noise Gate Calibration (First ~250ms)
        if (calibrationSamples < CALIBRATION_COUNT) {
            noiseFloor = (noiseFloor * calibrationSamples + inputRms) / (calibrationSamples + 1)
            calibrationSamples++
        }

        val output = ShortArray(buffer.size)
        val gateThreshold = noiseFloor * 1.5 // Relaxed gate
        
        // 2. Simple AGC Gain Calculation
        if (inputRms > gateThreshold && inputRms > 0) {
            val targetGain = (TARGET_RMS / inputRms).coerceIn(1.0, MAX_GAIN)
            currentGain = currentGain * (1.0 - GAIN_SMOOTHING) + targetGain * GAIN_SMOOTHING
        }

        for (i in buffer.indices) {
            var sample = buffer[i].toDouble()

            // 3. High-Pass Filter (120Hz)
            // y[n] = alpha * (y[n-1] + x[n] - x[n-1])
            val filteredY = HPF_ALPHA * (lastY + sample - lastX)
            lastX = sample
            lastY = filteredY
            sample = filteredY

            // 4. Pre-emphasis Filter (High-frequency boost for consonants)
            // y[n] = x[n] - 0.97 * x[n-1]
            val emphasisSample = sample - 0.97 * prevSample
            prevSample = sample.toInt().toShort()
            sample = emphasisSample
            
            // 5. Apply AGC Gain
            sample *= currentGain
            
            // 6. Simple Noise Gate / Soft Expansion
            if (inputRms < gateThreshold) {
                sample *= 0.1
            }

            output[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        return output
    }

    private fun calculateRMS(buffer: ShortArray): Double {
        var sum = 0.0
        for (sample in buffer) {
            sum += sample.toDouble() * sample.toDouble()
        }
        return if (buffer.isNotEmpty()) Math.sqrt(sum / buffer.size) else 0.0
    }

    private fun shortArrayToByteArray(shortArray: ShortArray): ByteArray {
        val byteArray = ByteArray(shortArray.size * 2)
        for (i in shortArray.indices) {
            val sample = shortArray[i]
            byteArray[i * 2] = (sample.toInt() and 0xFF).toByte()
            byteArray[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }
        return byteArray
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
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
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
