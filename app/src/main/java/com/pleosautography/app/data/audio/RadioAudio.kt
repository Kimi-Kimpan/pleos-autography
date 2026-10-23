package com.pleosautography.app.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 오토그래피 라디오의 재생 상태(앱 안 표시용).
 * @param paused 인터뷰·원고 듣기 중이라 잠시 멈춤.
 * @param ducked 알림 멜로디·오토 발화 중이라 소리를 줄임.
 */
data class RadioPlayback(val paused: Boolean = false, val ducked: Boolean = false)

/**
 * 오디오 포커스로 다른 소리를 다룬다.
 * - 알림 멜로디·오토 발화: 다른 소리를 잠시 줄인다(GAIN_TRANSIENT_MAY_DUCK).
 * - 인터뷰·원고 듣기: 다른 소리를 잠시 멈춘다(GAIN_TRANSIENT). 운전자 목소리를 또렷하게 듣기 위해서다.
 * 차의 다른 미디어도 시스템이 함께 줄이거나 멈추고, 앱 안 라디오 표시는 [radio]가 따른다.
 */
class RadioAudio(context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANT)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private val state = MutableStateFlow(RadioPlayback())
    val radio: StateFlow<RadioPlayback> = state.asStateFlow()

    private var pauseRequest: AudioFocusRequest? = null
    private var duckRequest: AudioFocusRequest? = null

    /** 인터뷰·원고 듣기를 시작·끝낼 때. */
    fun setTalking(talking: Boolean) {
        if (talking == state.value.paused) return
        if (talking) {
            pauseRequest = request(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        } else {
            pauseRequest?.let(audioManager::abandonAudioFocusRequest)
            pauseRequest = null
        }
        state.update { it.copy(paused = talking) }
    }

    /** 멜로디·오토 발화 동안 다른 소리를 줄인다. 인터뷰 중(이미 멈춤)이면 그대로 둔다. */
    suspend fun <T> ducking(block: suspend () -> T): T {
        val duck = !state.value.paused
        if (duck) {
            duckRequest = request(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            state.update { it.copy(ducked = true) }
        }
        try {
            return block()
        } finally {
            if (duck) {
                duckRequest?.let(audioManager::abandonAudioFocusRequest)
                duckRequest = null
                state.update { it.copy(ducked = false) }
            }
        }
    }

    private fun request(gain: Int): AudioFocusRequest =
        AudioFocusRequest.Builder(gain)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener { }
            .build()
            .also { audioManager.requestAudioFocus(it) }
}
