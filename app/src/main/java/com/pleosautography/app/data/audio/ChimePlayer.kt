package com.pleosautography.app.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * 오토가 먼저 말을 걸기 전에 울리는 짧은 알림 멜로디(CLAUDE.md 핵심 규칙 3).
 * 소리 파일 없이 코드로 만든 두 음(솔 → 도)이라 저작권 걱정이 없다.
 */
class ChimePlayer {

    private val samples: ShortArray by lazy { synthesize() }

    /** 멜로디를 끝까지 재생하고 돌아온다. 재생할 수 없으면 조용히 넘어간다. */
    suspend fun play() = withContext(Dispatchers.Default) {
        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(samples.size * 2)
                .build()
        }.getOrNull() ?: return@withContext
        try {
            track.write(samples, 0, samples.size)
            track.play()
            delay(samples.size * 1000L / SAMPLE_RATE + 50)
        } finally {
            track.release()
        }
    }

    private fun synthesize(): ShortArray {
        val notes = listOf(783.99, 1046.50) // G5, C6
        val noteSamples = SAMPLE_RATE * NOTE_MS / 1000
        val out = ShortArray(noteSamples * notes.size)
        notes.forEachIndexed { n, freq ->
            for (i in 0 until noteSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                // 부드럽게 시작하고 길게 사라지는 소리(클릭 잡음 없이).
                val attack = min(1.0, i / (SAMPLE_RATE * 0.01))
                val decay = 1.0 - i.toDouble() / noteSamples
                val value = sin(2 * PI * freq * t) * attack * decay * decay * VOLUME
                out[n * noteSamples + i] = (value * Short.MAX_VALUE).toInt().toShort()
            }
        }
        return out
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
        const val NOTE_MS = 260
        const val VOLUME = 0.45
    }
}
