package com.pleosautography.app.data.speech

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.log10
import kotlin.math.sqrt

/** 시연용 받아쓰기 문장(assets/demo/answers.json). */
class DemoScript(val seed: Map<String, String>, private val answers: Map<String, String>, private val fallback: String) {
    fun answerFor(questionId: String?): String = questionId?.let(answers::get) ?: fallback

    companion object {
        fun load(context: Context): DemoScript {
            val json = JSONObject(context.assets.open("demo/answers.json").bufferedReader().use { it.readText() })
            fun JSONObject.toMap() = keys().asSequence().associateWith { getString(it) }
            return DemoScript(json.getJSONObject("seed").toMap(), json.getJSONObject("answers").toMap(), json.getString("fallback"))
        }
    }
}

/**
 * 시연용 음성 입력. 마이크 음량은 실제로 읽어 파형을 움직이고,
 * 말이 시작됐다가 [SILENCE_END_MS] 동안 조용해지면 준비된 문장을 받아 적는다.
 * Pleos SpeechToText 승인 전까지 쓴다.
 */
class FakeSpeechInput(private val context: Context, private val script: DemoScript) : SpeechInput {

    private val utteranceFlow = MutableSharedFlow<String>(extraBufferCapacity = 8)
    override val utterances: SharedFlow<String> = utteranceFlow.asSharedFlow()

    private val injected = Channel<String>(Channel.CONFLATED)
    private val finish = Channel<Unit>(Channel.CONFLATED)

    @Volatile private var listening = false

    override fun inject(text: String) {
        if (listening) injected.trySend(text) else utteranceFlow.tryEmit(text)
    }

    override fun finishNow() {
        if (listening) finish.trySend(Unit)
    }

    override suspend fun listen(questionId: String?, onLevel: (Float) -> Unit): Heard = withContext(Dispatchers.IO) {
        injected.tryReceive()
        finish.tryReceive()
        listening = true
        val record = openRecord()
        val started = System.currentTimeMillis()
        var speechStart = 0L
        var lastVoice = 0L
        try {
            record?.startRecording()
            val buffer = ShortArray(FRAME)
            while (isActive) {
                // adb로 들어온 말·“말 끝” 신호를 먼저 본다.
                injected.tryReceive().getOrNull()?.let { return@withContext Heard.Text(it, seconds(started)) }
                if (finish.tryReceive().isSuccess) return@withContext Heard.Text(script.answerFor(questionId), seconds(started))
                if (record == null) delay(100)

                val now = System.currentTimeMillis()
                val level = record?.let { readLevel(it, buffer) } ?: 0f
                onLevel(level)
                if (level >= VOICE_LEVEL) {
                    if (speechStart == 0L) speechStart = now
                    lastVoice = now
                }
                val spoke = speechStart != 0L && lastVoice - speechStart >= MIN_SPEECH_MS
                if (spoke && now - lastVoice >= SILENCE_END_MS) {
                    return@withContext Heard.Text(script.answerFor(questionId), ((lastVoice - speechStart) / 1000).toInt())
                }
                if (now - started >= TIMEOUT_MS) return@withContext Heard.Silence
            }
            Heard.Silence
        } finally {
            listening = false
            record?.runCatching { stop(); release() }
            onLevel(0f)
        }
    }

    @SuppressLint("MissingPermission")
    private fun openRecord(): AudioRecord? {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "마이크 권한 없음: 음량 없이 adb 이벤트만 받는다")
            return null
        }
        val size = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val record = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, size * 2)
        return record.takeIf { it.state == AudioRecord.STATE_INITIALIZED } ?: run { record.release(); null }
    }

    /** -60 dBFS → 0, -20 dBFS → 1. */
    private fun readLevel(record: AudioRecord, buffer: ShortArray): Float {
        val n = record.read(buffer, 0, buffer.size)
        if (n <= 0) return 0f
        var sum = 0.0
        for (i in 0 until n) sum += buffer[i] * buffer[i].toDouble()
        val rms = sqrt(sum / n)
        val db = if (rms > 0) 20 * log10(rms / Short.MAX_VALUE) else -120.0
        return ((db + 60) / 40).toFloat().coerceIn(0f, 1f)
    }

    private fun seconds(since: Long) = ((System.currentTimeMillis() - since) / 1000).toInt()

    private companion object {
        const val TAG = "FakeSpeechInput"
        const val RATE = 16_000
        const val FRAME = RATE / 20 // 50ms
        const val VOICE_LEVEL = 0.35f
        const val MIN_SPEECH_MS = 400L
        const val SILENCE_END_MS = 1_500L
        const val TIMEOUT_MS = 60_000L
    }
}

/**
 * 시연용 음성 출력: 소리를 내지 않고 문장을 읽는 시간만큼 기다린다.
 * 시연 영상에서는 이 시간에 맞춰 오토 목소리를 편집으로 입힌다.
 */
class CaptionSpeechOutput : SpeechOutput {
    private val state = MutableStateFlow(false)
    override val speaking: StateFlow<Boolean> = state.asStateFlow()

    override suspend fun speak(text: String) {
        state.value = true
        try {
            delay(estimateMs(text))
        } finally {
            state.value = false
        }
    }

    companion object {
        /** 한국어 낭독 속도(약 초당 7음절)로 잡은 길이. */
        fun estimateMs(text: String): Long = (400L + text.count { !it.isWhitespace() } * 140L).coerceAtMost(9_000L)
    }
}
