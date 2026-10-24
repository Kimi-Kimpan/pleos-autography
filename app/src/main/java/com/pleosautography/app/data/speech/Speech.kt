package com.pleosautography.app.data.speech

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** 한 번 들은 결과. */
sealed interface Heard {
    data class Text(val text: String, val durationSec: Int) : Heard

    /** 아무 말도 없이 시간이 지났다. */
    data object Silence : Heard
}

/**
 * 음성 입력 어댑터(CLAUDE.md의 SDK 어댑터 패턴).
 * - Pleos 구현체: SpeechToText SDK(앱 등록 승인 뒤).
 * - [FakeSpeechInput]: 실제 마이크 레벨 + 준비된 문장(시연용).
 */
interface SpeechInput {
    /** 인터뷰 밖에서 들린 말(“응”, “오토야, 뭐해?” 등). 화면이 명령으로 해석한다. */
    val utterances: SharedFlow<String>

    /**
     * 말이 끝날 때까지 듣는다.
     * @param questionId 지금 질문(시연용 Fake가 받아 적을 문장을 고르는 데 쓴다).
     * @param onLevel 입력 음량 0~1(음성 파형).
     */
    suspend fun listen(questionId: String?, onLevel: (Float) -> Unit): Heard

    /** 밖에서 들린 말을 넣는다(adb 데모 이벤트). 듣는 중이면 그 답이 되고, 아니면 [utterances]로 간다. */
    fun inject(text: String)

    /** 지금 말이 끝난 것으로 본다(시연에서 침묵 감지를 기다리지 않을 때). */
    fun finishNow()
}

/**
 * 음성 출력 어댑터.
 * - Pleos 구현체: TextToSpeech SDK(앱 등록 승인 뒤).
 * - [CaptionSpeechOutput]: 소리 없이 말하는 시간만 기다린다(시연 영상에서 목소리를 편집으로 입힌다).
 */
interface SpeechOutput {
    val speaking: StateFlow<Boolean>
    suspend fun speak(text: String)
}

/**
 * 음성 설정.
 * @param silenceEndMs 말이 끝난 것으로 볼 침묵 길이. 기본 1.5초, 시연(디버그 빌드)은 1초.
 *   환경설정 화면에서 바꿀 수 있게 할 값이다.
 */
class VoiceSettings(@Volatile var silenceEndMs: Long = DEFAULT_SILENCE_END_MS) {
    companion object {
        const val DEFAULT_SILENCE_END_MS = 1_500L
        const val DEMO_SILENCE_END_MS = 1_000L
    }
}
