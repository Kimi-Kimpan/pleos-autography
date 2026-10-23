package com.pleosautography.app.sdkcheck

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.util.TypedValue
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * 음성 가능 여부 확인용 화면(디버그 빌드 전용).
 * 1) TTS 엔진과 한국어 지원  2) 마이크 원음 레벨(AudioRecord)  3) 음성 인식(한국어)
 * adb로도 실행할 수 있다: --es step tts|mic|stt
 */
class VoiceCheckActivity : ComponentActivity() {

    private lateinit var logText: TextView
    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null

    private val requestMic = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        log("RECORD_AUDIO 권한: ${if (granted) "허용" else "거부"}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContentView())
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestMic.launch(Manifest.permission.RECORD_AUDIO)
        }
        runStep(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        runStep(intent)
    }

    override fun onDestroy() {
        tts?.shutdown()
        recognizer?.destroy()
        super.onDestroy()
    }

    private fun runStep(intent: Intent) {
        when (intent.getStringExtra("step")) {
            "tts" -> checkTts(intent.getStringExtra("engine"))
            "mic" -> checkMic()
            "stt" -> checkStt()
            "tone" -> checkTone(intent.getStringExtra("usage") ?: "media")
        }
    }

    // 1) TTS
    private fun checkTts(engine: String?) {
        tts?.shutdown()
        log("TTS 시작 (엔진: ${engine ?: "기본"})")
        lateinit var created: TextToSpeech
        created = TextToSpeech(this, { status ->
            if (status != TextToSpeech.SUCCESS) {
                log("TTS 초기화 실패: $status")
                return@TextToSpeech
            }
            log("엔진 목록: ${created.engines.joinToString { it.name }}")
            log("기본 엔진: ${created.defaultEngine}")
            log("한국어 지원: ${langName(created.isLanguageAvailable(Locale.KOREAN))}")
            val koVoices = runCatching { created.voices?.filter { it.locale.language == "ko" } }.getOrNull()
            log("한국어 목소리: ${koVoices?.joinToString { "${it.name}(${if (it.isNetworkConnectionRequired) "네트워크" else "기기"})" } ?: "조회 불가"}")
            val set = created.setLanguage(Locale.KOREAN)
            log("setLanguage(ko): ${langName(set)}")
            created.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String) = log("발화 시작")
                override fun onDone(id: String) = log("발화 끝")
                @Deprecated("Deprecated in Java")
                override fun onError(id: String) = log("발화 오류")
                override fun onError(id: String, errorCode: Int) = log("발화 오류: $errorCode")
            })
            created.speak("안녕하세요, 저는 오토예요. 처음 운전대를 잡은 날, 기억나세요?", TextToSpeech.QUEUE_FLUSH, null, "probe")
        }, engine)
        tts = created
    }

    private fun langName(code: Int) = when (code) {
        TextToSpeech.LANG_AVAILABLE -> "가능"
        TextToSpeech.LANG_COUNTRY_AVAILABLE -> "가능(국가)"
        TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> "가능(변형)"
        TextToSpeech.LANG_MISSING_DATA -> "데이터 없음"
        TextToSpeech.LANG_NOT_SUPPORTED -> "지원 안 함"
        else -> "알 수 없음($code)"
    }

    // 2) 마이크 원음 레벨: 음성 인식과 상관없이 Mac 마이크 소리가 들어오는지 본다.
    private fun checkMic() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            log("마이크 권한 없음")
            return
        }
        log("마이크 6초 녹음 시작(소리를 내 보세요)")
        thread(name = "mic-probe") {
            val rate = 16000
            val size = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * 2
            @Suppress("MissingPermission")
            val record = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, size)
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                log("AudioRecord 초기화 실패")
                return@thread
            }
            val buffer = ShortArray(rate / 2)
            record.startRecording()
            repeat(12) {
                val n = record.read(buffer, 0, buffer.size)
                var sum = 0.0
                var peak = 0
                for (i in 0 until n) {
                    sum += buffer[i] * buffer[i].toDouble()
                    peak = maxOf(peak, kotlin.math.abs(buffer[i].toInt()))
                }
                val rms = if (n > 0) sqrt(sum / n) else 0.0
                val db = if (rms > 0) 20 * log10(rms / Short.MAX_VALUE) else -120.0
                log("마이크 %.1fs: %.1f dBFS, 최대 %d".format((it + 1) * 0.5, db, peak))
            }
            record.stop()
            record.release()
            log("마이크 녹음 끝")
        }
    }

    // 4) 소리 출력: 2초 동안 440Hz를 [usage]로 재생한다. Mac 스피커로 나오면 마이크 레벨(step mic)이 오른다.
    private fun checkTone(usage: String) {
        val usageValue = when (usage) {
            "assistant" -> android.media.AudioAttributes.USAGE_ASSISTANT
            "guidance" -> android.media.AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE
            "notification" -> android.media.AudioAttributes.USAGE_NOTIFICATION
            else -> android.media.AudioAttributes.USAGE_MEDIA
        }
        thread(name = "tone-probe") {
            val rate = 44100
            val samples = ShortArray(rate * 2) { i -> (kotlin.math.sin(2 * Math.PI * 440 * i / rate) * 0.6 * Short.MAX_VALUE).toInt().toShort() }
            val track = android.media.AudioTrack.Builder()
                .setAudioAttributes(android.media.AudioAttributes.Builder().setUsage(usageValue).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(android.media.AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(samples.size * 2)
                .build()
            track.write(samples, 0, samples.size)
            track.play()
            log("소리 재생: $usage (2초)")
            Thread.sleep(2200)
            track.release()
            log("소리 끝: $usage")
        }
    }

    // 3) 음성 인식
    private fun checkStt() {
        log("인식 서비스 있음: ${SpeechRecognizer.isRecognitionAvailable(this)}")
        recognizer?.destroy()
        val created = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer = created
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            created.checkRecognitionSupport(intent, Executors.newSingleThreadExecutor(), object : RecognitionSupportCallback {
                override fun onSupportResult(support: RecognitionSupport) = log(
                    "인식 지원: 설치됨=${support.installedOnDeviceLanguages} 설치 가능=${support.pendingOnDeviceLanguages + support.supportedOnDeviceLanguages} 온라인=${support.onlineLanguages}",
                )
                override fun onError(error: Int) = log("인식 지원 조회 오류: ${sttError(error)}")
            })
        }
        var lastRmsLog = 0L
        created.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = log("인식: 말씀하세요")
            override fun onBeginningOfSpeech() = log("인식: 말 시작 감지")
            override fun onRmsChanged(rmsdB: Float) {
                val now = System.currentTimeMillis()
                if (now - lastRmsLog > 500) {
                    lastRmsLog = now
                    log("인식 RMS: %.1f dB".format(rmsdB))
                }
            }
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = log("인식: 말 끝")
            override fun onError(error: Int) = log("인식 오류: ${sttError(error)}")
            override fun onResults(results: Bundle?) =
                log("인식 결과: ${results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)}")
            override fun onPartialResults(partial: Bundle?) =
                log("부분 결과: ${partial?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)}")
            override fun onEvent(eventType: Int, params: Bundle?) = log("인식 이벤트: $eventType")
        })
        created.startListening(intent)
    }

    private fun sttError(code: Int) = when (code) {
        SpeechRecognizer.ERROR_NETWORK -> "네트워크"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "네트워크 시간 초과"
        SpeechRecognizer.ERROR_AUDIO -> "오디오"
        SpeechRecognizer.ERROR_SERVER -> "서버"
        SpeechRecognizer.ERROR_CLIENT -> "클라이언트"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "말 없음(시간 초과)"
        SpeechRecognizer.ERROR_NO_MATCH -> "일치 없음"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "사용 중"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "권한 없음"
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "언어 지원 안 함"
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "언어 사용 불가"
        SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "서버 연결 끊김"
        else -> "기타($code)"
    }

    private fun log(message: String) {
        Log.i(TAG, message)
        runOnUiThread { logText.append("$message\n") }
    }

    private fun buildContentView(): LinearLayout {
        logText = TextView(this).apply { setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f) }
        fun button(label: String, step: String) = Button(this).apply {
            text = label
            setOnClickListener { runStep(Intent().putExtra("step", step)) }
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            fitsSystemWindows = true
            setPadding(48, 48, 48, 48)
            addView(TextView(this@VoiceCheckActivity).apply {
                text = "음성 확인: TTS · 마이크 · 음성 인식"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            })
            addView(button("1. TTS 말하기", "tts"))
            addView(button("2. 마이크 레벨 6초", "mic"))
            addView(button("3. 음성 인식", "stt"))
            addView(ScrollView(this@VoiceCheckActivity).apply { addView(logText) })
        }
    }

    private companion object {
        const val TAG = "VoiceCheck"
    }
}
