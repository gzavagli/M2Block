package com.gzavagli.m2block.utils

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class AudioSynthManager {
    var isMuted: Boolean = false

    private val sampleRate = 22050 // Retro game sample rate

    private fun playTone(
        frequencyStart: Float,
        frequencyEnd: Float,
        durationMs: Int,
        waveType: String = "sine"
    ) {
        if (isMuted) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
                val samples = ShortArray(numSamples)
                
                for (i in 0 until numSamples) {
                    val progress = i.toFloat() / numSamples
                    val currentFreq = frequencyStart + (frequencyEnd - frequencyStart) * progress
                    
                    val time = i.toFloat() / sampleRate
                    val angle = 2.0 * PI * currentFreq * time
                    
                    val value = when (waveType) {
                        "triangle" -> {
                            val t = (angle / (2.0 * PI) % 1.0).toFloat()
                            val raw = if (t < 0.5f) 4f * t - 1f else 3f - 4f * t
                            (raw * Short.MAX_VALUE * 0.15f).toInt().toShort()
                        }
                        "sawtooth" -> {
                            val t = (angle / (2.0 * PI) % 1.0).toFloat()
                            val raw = 2f * t - 1f
                            (raw * Short.MAX_VALUE * 0.08f).toInt().toShort()
                        }
                        else -> { // "sine"
                            val envelope = 1f - progress
                            val raw = sin(angle)
                            (raw * Short.MAX_VALUE * 0.15f * envelope).toInt().toShort()
                        }
                    }
                    samples[i] = value
                }

                val bufferSize = numSamples * 2
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(samples, 0, numSamples, AudioTrack.WRITE_NON_BLOCKING)
                audioTrack.play()
                
                Thread.sleep(durationMs.toLong() + 50)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playDrop() {
        playTone(320f, 100f, 180, "sine")
    }

    fun playLand() {
        playTone(80f, 40f, 50, "triangle")
    }

    fun playMerge(combo: Int = 1) {
        val scale = floatArrayOf(523.25f, 587.33f, 659.25f, 698.46f, 783.99f, 880.00f, 987.77f, 1046.50f)
        val baseFreq = scale[Math.min(combo - 1, scale.size - 1)]
        playTone(baseFreq, baseFreq * 1.05f, 280, "sine")
    }

    fun playPowerup() {
        CoroutineScope(Dispatchers.IO).launch {
            val notes = floatArrayOf(440f, 554.37f, 659.25f, 880f)
            notes.forEachIndexed { index, freq ->
                if (isMuted) return@forEachIndexed
                playTone(freq, freq, 120, "triangle")
                Thread.sleep(80)
            }
        }
    }

    fun playGameOver() {
        CoroutineScope(Dispatchers.IO).launch {
            val chord = floatArrayOf(196f, 233.08f, 293.66f)
            chord.forEach { freq ->
                if (isMuted) return@forEach
                playTone(freq, freq * 0.5f, 950, "sawtooth")
            }
        }
    }
}
