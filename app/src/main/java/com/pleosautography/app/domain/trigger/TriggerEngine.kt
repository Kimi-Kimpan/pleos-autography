package com.pleosautography.app.domain.trigger

import com.pleosautography.app.data.navi.NaviSnapshot

/** 오토가 먼저 대화를 제안하는 이유. */
enum class NudgeReason {
    /** 정체(저속) 구간에 15분 넘게 머묾. */
    CONGESTION,

    /** 긴 직진 구간에 들어선 지 15분. */
    STRAIGHT,

    /** 복잡한 구간 때문에 미뤘던 대화를 다시 제안. */
    RESUME,
}

/** 트리거 엔진이 내리는 결정. 화면 전환·소리는 UI가 한다. */
sealed interface TriggerAction {
    /** 알림 멜로디 뒤에 대화를 제안한다. */
    data class Nudge(val reason: NudgeReason) : TriggerAction

    /** 복잡한 구간: 인터뷰를 멈추고 “조금 이따가 이어 할까요?”라고 미룬다. */
    data object Defer : TriggerAction

    /** 목적지까지 10분 이하: 새 질문 없이 마무리 화면을 띄운다. */
    data class WrapUp(val minutesToArrival: Int) : TriggerAction

    /** 떠 있는 제안을 닫는다(도착이 가깝거나 복잡한 구간에 들어섬). */
    data object CloseNudge : TriggerAction

    /** 연속 주행이 길다: 휴식을 권한다. 대화가 휴식을 대신하지 않는다. */
    data object SuggestRest : TriggerAction
}

/**
 * 트리거 판단 입력 한 순간.
 * @param interviewActive 인터뷰(또는 원고 듣기)가 열려 있는지.
 * @param nudgeVisible 제안 화면이 떠 있는지.
 * @param proactiveTalk 환경설정 ‘오토가 먼저 말 걸기’. 끄면 정체·직진 제안을 하지 않는다(미룬 대화 다시 제안·마무리·휴식은 그대로).
 */
data class TriggerInput(
    val nowMs: Long,
    val driving: Boolean,
    val speedKmh: Float?,
    val navi: NaviSnapshot,
    val interviewActive: Boolean,
    val nudgeVisible: Boolean,
    val proactiveTalk: Boolean = true,
)

data class TriggerConfig(
    val dwellMs: Long = 15 * MINUTE,
    val slowSpeedKmh: Float = 20f,
    val wrapUpMinutes: Int = 10,
    val nudgeCooldownMs: Long = 20 * MINUTE,
    val restAfterMs: Long = 2 * 60 * MINUTE,
) {
    companion object {
        const val MINUTE = 60_000L
    }
}

/**
 * 상황 트리거(CLAUDE.md 핵심 규칙 3·4). 입력을 받을 때마다 [evaluate]로 할 일을 정한다.
 * 안드로이드에 기대지 않는 순수 Kotlin이라 단위 테스트로 규칙을 확인한다.
 *
 * 우선순위: 도착 10분 전 마무리 > 복잡 구간 미루기 > 미룬 대화 다시 제안 > 휴식 권유 > 정체·직진 제안.
 */
class TriggerEngine(private val config: TriggerConfig = TriggerConfig()) {

    private var slowSince: Long? = null
    private var drivingSince: Long? = null
    private var lastNudgeAt: Long? = null
    private var deferred = false
    private var wrappedUp = false
    private var restSuggested = false

    /** 시연에서 같은 제안을 바로 다시 보이도록 대기 시간을 지운다. */
    fun clearCooldown() {
        lastNudgeAt = null
    }

    fun evaluate(input: TriggerInput): List<TriggerAction> {
        val now = input.nowMs
        if (!input.driving) {
            slowSince = null
            drivingSince = null
            deferred = false
            wrappedUp = false
            restSuggested = false
            return emptyList()
        }
        if (drivingSince == null) drivingSince = now
        val slow = input.speedKmh != null && input.speedKmh < config.slowSpeedKmh
        slowSince = if (slow) slowSince ?: now else null

        val navi = input.navi
        val actions = mutableListOf<TriggerAction>()

        // 1) 도착 10분 전: 새 질문 없이 마무리한다.
        val minutes = navi.minutesToArrival
        if (navi.guiding && minutes != null && minutes <= config.wrapUpMinutes) {
            if (input.interviewActive && !wrappedUp) actions += TriggerAction.WrapUp(minutes)
            if (input.nudgeVisible) actions += TriggerAction.CloseNudge
            wrappedUp = true
            deferred = false
            return actions
        }
        if (minutes == null || minutes > config.wrapUpMinutes) wrappedUp = false

        // 2) 복잡한 구간: 대화를 미루고 새 제안도 하지 않는다.
        if (navi.guiding && navi.complexSection) {
            if (input.interviewActive) {
                actions += TriggerAction.Defer
                deferred = true
            }
            if (input.nudgeVisible) actions += TriggerAction.CloseNudge
            return actions
        }

        // 3) 구간을 지나면 미뤘던 대화를 다시 제안한다.
        if (deferred && !input.interviewActive && !input.nudgeVisible) {
            deferred = false
            lastNudgeAt = now
            return listOf(TriggerAction.Nudge(NudgeReason.RESUME))
        }

        // 4) 연속 주행이 길면 휴식을 권한다(한 번).
        val since = drivingSince
        if (!restSuggested && since != null && now - since >= config.restAfterMs) {
            restSuggested = true
            actions += TriggerAction.SuggestRest
        }

        // 5) 정체·긴 직진 15분: 인터뷰 중이 아니고 대기 시간이 지났을 때만.
        val coolDownOver = lastNudgeAt?.let { now - it >= config.nudgeCooldownMs } ?: true
        if (input.proactiveTalk && !input.interviewActive && !input.nudgeVisible && coolDownOver) {
            val reason = when {
                slowSince?.let { now - it >= config.dwellMs } == true -> NudgeReason.CONGESTION
                navi.straightSinceMs?.let { now - it >= config.dwellMs } == true -> NudgeReason.STRAIGHT
                else -> null
            }
            if (reason != null) {
                lastNudgeAt = now
                if (reason == NudgeReason.CONGESTION) slowSince = now
                actions += TriggerAction.Nudge(reason)
            }
        }
        return actions
    }
}
