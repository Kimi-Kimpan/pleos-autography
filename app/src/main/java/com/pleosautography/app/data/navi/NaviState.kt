package com.pleosautography.app.data.navi

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 경로 안내 상태 한 순간.
 * @param guiding 경로 안내 중인지. 안내 중이 아니면 도착·복잡 구간·직진 판단을 하지 않는다.
 * @param minutesToArrival 목적지까지 남은 시간(분). 모르면 null.
 * @param complexSection 회전·합류가 이어지는 복잡한 구간에 있는지.
 * @param straightSinceMs 지금 직진 구간에 들어선 시각(앱 시계 기준). 직진이 아니면 null.
 */
data class NaviSnapshot(
    val guiding: Boolean = false,
    val minutesToArrival: Int? = null,
    val complexSection: Boolean = false,
    val straightSinceMs: Long? = null,
)

/**
 * 경로 안내 어댑터(CLAUDE.md의 SDK 어댑터 패턴).
 * - Pleos 구현체(마일스톤 8): NaviHelper 3.0.5의 DrivingInfo(남은 시간)·TBTInfo(다음 회전)로 만든다.
 *   단위(초/분, 미터)는 에뮬레이터에서 확인한 뒤 붙인다.
 * - [FakeNaviState]: 데모 이벤트로 상황을 재현한다.
 */
interface NaviState {
    val snapshot: StateFlow<NaviSnapshot>
}

/** 시연용 경로 안내. 시작하면 목적지까지 24분 남은 안내 중 상태다. */
class FakeNaviState : NaviState {
    private val state = MutableStateFlow(Initial)
    override val snapshot: StateFlow<NaviSnapshot> = state.asStateFlow()

    fun setComplexSection(complex: Boolean) = state.update { it.copy(complexSection = complex) }

    fun setMinutesToArrival(minutes: Int?) = state.update { it.copy(minutesToArrival = minutes) }

    fun setStraightSince(timeMs: Long?) = state.update { it.copy(straightSinceMs = timeMs) }

    fun reset() {
        state.value = Initial
    }

    companion object {
        val Initial = NaviSnapshot(guiding = true, minutesToArrival = 24)
    }
}
