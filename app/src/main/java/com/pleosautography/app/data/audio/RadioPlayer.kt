package com.pleosautography.app.data.audio

import com.pleosautography.app.data.demo.DemoData
import com.pleosautography.app.data.demo.RadioEpisode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 오토그래피 라디오 재생 위치.
 * @param userPaused 운전자가 일시정지를 눌렀다. 인터뷰 중 잠시 멈춤은 [RadioPlayback.paused]가 따로 정한다.
 */
data class RadioState(
    val playlist: List<RadioEpisode> = DemoData.radio.playlist,
    val index: Int = 0,
    val positionSec: Int = DemoData.radio.startSec,
    val userPaused: Boolean = false,
) {
    val episode: RadioEpisode get() = playlist[index]

    /** 다음으로 들을 이야기 2편. */
    val upNext: List<RadioEpisode> get() = (1..2).map { playlist[(index + it) % playlist.size] }

    val progress: Float get() = if (episode.durationSec == 0) 0f else positionSec.toFloat() / episode.durationSec
}

/**
 * 오토그래피 라디오(시연판). 낭독 음원은 2단계 범위라 소리 없이 재생 위치만 1초씩 움직인다.
 * 인터뷰·원고 듣기로 잠시 멈췄을 때([RadioAudio.radio]의 paused)는 위치가 서 있고, 끝나면 이어서 간다.
 */
class RadioPlayer(scope: CoroutineScope, private val audio: RadioAudio) {
    private val mutable = MutableStateFlow(RadioState())
    val state: StateFlow<RadioState> = mutable.asStateFlow()

    /** 지금 실제로 흐르는지(운전자 일시정지도, 인터뷰 중 멈춤도 아닐 때). */
    val playing: Boolean get() = !mutable.value.userPaused && !audio.radio.value.paused

    init {
        scope.launch {
            while (true) {
                delay(1_000)
                if (playing) tick()
            }
        }
    }

    /** 재생·일시정지. 인터뷰 중 잠시 멈춘 동안은 누르지 않은 것으로 둔다(인터뷰가 끝나면 이어진다). */
    fun togglePlay() {
        if (audio.radio.value.paused) return
        mutable.update { it.copy(userPaused = !it.userPaused) }
    }

    fun next() = mutable.update { it.copy(index = (it.index + 1) % it.playlist.size, positionSec = 0) }

    fun rewind15() = mutable.update { it.copy(positionSec = (it.positionSec - REWIND_SEC).coerceAtLeast(0)) }

    private fun tick() = mutable.update {
        if (it.positionSec + 1 >= it.episode.durationSec) {
            it.copy(index = (it.index + 1) % it.playlist.size, positionSec = 0)
        } else {
            it.copy(positionSec = it.positionSec + 1)
        }
    }

    companion object {
        const val REWIND_SEC = 15

        fun formatTime(sec: Int): String = "%d:%02d".format(sec / 60, sec % 60)
    }
}
