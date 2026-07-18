package com.example.ui

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

object SoundEffects {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun playMove() {
        scope.launch {
            playDecayingTone(150f, 75f, 0.08f, 0.25f)
        }
    }

    fun playCapture() {
        scope.launch {
            playDecayingTone(700f, 350f, 0.06f, 0.3f)
        }
    }

    fun playCheck() {
        scope.launch {
            playTone(523.25f, 0.08f, 0.25f) // C5
            kotlinx.coroutines.delay(50)
            playTone(659.25f, 0.12f, 0.3f)  // E5
        }
    }

    fun playCheckmate() {
        scope.launch {
            val notes = listOf(523.25f, 659.25f, 783.99f, 1046.50f) // C5, E5, G5, C6 (major triad rising)
            for (note in notes) {
                playTone(note, 0.15f, 0.35f)
                kotlinx.coroutines.delay(120)
            }
        }
    }

    private fun playTone(frequency: Float, durationSec: Float, volume: Float) {
        try {
            val sampleRate = 22050
            val numSamples = (durationSec * sampleRate).toInt()
            if (numSamples <= 0) return
            val sample = DoubleArray(numSamples)
            val generatedSnd = ByteArray(2 * numSamples)

            for (i in 0 until numSamples) {
                sample[i] = sin(2.0 * Math.PI * i / (sampleRate / frequency))
            }

            var idx = 0
            for (dVal in sample) {
                val valShort = (dVal * 32767 * volume).toInt().toShort()
                generatedSnd[idx++] = (valShort.toInt() and 0x00ff).toByte()
                generatedSnd[idx++] = ((valShort.toInt() and 0xff00) ushr 8).toByte()
            }

            val audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                generatedSnd.size,
                AudioTrack.MODE_STATIC
            )
            audioTrack.write(generatedSnd, 0, generatedSnd.size)
            audioTrack.play()
            audioTrack.release()
        } catch (e: Exception) {
            // Silent fallback in case of hardware config limits
        }
    }

    private fun playDecayingTone(startFreq: Float, endFreq: Float, durationSec: Float, volume: Float) {
        try {
            val sampleRate = 22050
            val numSamples = (durationSec * sampleRate).toInt()
            if (numSamples <= 0) return
            val sample = DoubleArray(numSamples)
            val generatedSnd = ByteArray(2 * numSamples)

            for (i in 0 until numSamples) {
                val t = i.toFloat() / numSamples
                val currentFreq = startFreq + (endFreq - startFreq) * t
                val decay = 1.0f - t
                sample[i] = sin(2.0 * Math.PI * i / (sampleRate / currentFreq)) * decay
            }

            var idx = 0
            for (dVal in sample) {
                val valShort = (dVal * 32767 * volume).toInt().toShort()
                generatedSnd[idx++] = (valShort.toInt() and 0x00ff).toByte()
                generatedSnd[idx++] = ((valShort.toInt() and 0xff00) ushr 8).toByte()
            }

            val audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                generatedSnd.size,
                AudioTrack.MODE_STATIC
            )
            audioTrack.write(generatedSnd, 0, generatedSnd.size)
            audioTrack.play()
            audioTrack.release()
        } catch (e: Exception) {
            // Silent fallback
        }
    }
}
