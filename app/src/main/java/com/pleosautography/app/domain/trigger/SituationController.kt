package com.pleosautography.app.domain.trigger

import android.util.Log
import com.pleosautography.app.data.navi.FakeNaviState
import com.pleosautography.app.data.navi.NaviSnapshot
import com.pleosautography.app.data.vehicle.DemoVehicleState
import com.pleosautography.app.data.vehicle.Gear
import com.pleosautography.app.data.vehicle.VehicleSnapshot
import com.pleosautography.app.domain.DriveMode
import com.pleosautography.app.domain.DriveModeRule
import com.pleosautography.app.domain.demo.DemoEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** 앱 시계. 시연에서는 [advance]로 “15분 뒤”를 바로 만든다. */
class AppClock {
    private var offsetMs = 0L
    fun now(): Long = System.currentTimeMillis() + offsetMs
    fun advance(ms: Long) {
        offsetMs += ms
    }
}

/** 주행 홈 인사말 자리에 잠시 보일 안내. */
enum class DriveNotice(val text: String) {
    DEFERRED("지금 앞길이 복잡하니\n조금 이따가 이어 할까요?"),
    REST("오래 달리셨어요.\n잠시 쉬어 가실까요?"),
}

/**
 * 차량·경로 안내 상태를 모아 [TriggerEngine]에 넣고, 결정을 화면에 전한다.
 * 5초마다, 그리고 상태가 바뀔 때마다 판단한다. 데모 이벤트(정체 15분 등)도 여기서 재현한다.
 */
class SituationController(
    private val scope: CoroutineScope,
    private val vehicle: DemoVehicleState,
    private val fakeNavi: FakeNaviState,
    demoEvents: SharedFlow<DemoEvent>,
    private val clock: AppClock = AppClock(),
    private val engine: TriggerEngine = TriggerEngine(),
) {
    val navi: StateFlow<NaviSnapshot> = fakeNavi.snapshot

    private val actionFlow = MutableSharedFlow<TriggerAction>(extraBufferCapacity = 8)

    /** 화면이 따라야 할 결정. */
    val actions: SharedFlow<TriggerAction> = actionFlow.asSharedFlow()

    private val reasonState = MutableStateFlow(NudgeReason.CONGESTION)

    /** 마지막 제안의 이유(제안 화면 문구). */
    val nudgeReason: StateFlow<NudgeReason> = reasonState.asStateFlow()

    private val noticeState = MutableStateFlow<DriveNotice?>(null)

    /** 주행 홈에 잠시 보일 안내(미루기·휴식). */
    val notice: StateFlow<DriveNotice?> = noticeState.asStateFlow()

    private var interviewActive = false
    private var nudgeVisible = false

    init {
        scope.launch { combine(vehicle.snapshot, fakeNavi.snapshot) { _, _ -> }.collect { evaluate() } }
        scope.launch {
            while (true) {
                delay(TICK_MS)
                evaluate()
            }
        }
        scope.launch { demoEvents.collect(::onDemoEvent) }
    }

    /** 지금 화면을 알린다. 인터뷰·제안이 열렸는지가 판단에 들어간다. */
    fun onScreen(interviewActive: Boolean, nudgeVisible: Boolean) {
        if (interviewActive || nudgeVisible) noticeState.value = null
        this.interviewActive = interviewActive
        this.nudgeVisible = nudgeVisible
        evaluate()
    }

    private fun evaluate() {
        val snapshot = vehicle.snapshot.value
        val driving = DriveModeRule.resolve(snapshot) == DriveMode.DRIVING
        if (!driving) noticeState.value = null
        val actions = engine.evaluate(
            TriggerInput(
                nowMs = clock.now(),
                driving = driving,
                speedKmh = snapshot.speedKmh,
                navi = fakeNavi.snapshot.value,
                interviewActive = interviewActive,
                nudgeVisible = nudgeVisible,
            ),
        )
        actions.forEach { action ->
            Log.i(TAG, "트리거: $action")
            when (action) {
                is TriggerAction.Nudge -> reasonState.value = action.reason
                TriggerAction.Defer -> noticeState.value = DriveNotice.DEFERRED
                TriggerAction.SuggestRest -> noticeState.value = DriveNotice.REST
                else -> Unit
            }
            actionFlow.tryEmit(action)
        }
    }

    private fun onDemoEvent(event: DemoEvent) {
        when (event) {
            DemoEvent.DRIVE, DemoEvent.REAL_VEHICLE -> fakeNavi.reset()
            DemoEvent.CONGESTION_15 -> {
                vehicle.override(VehicleSnapshot(Gear.DRIVE, speedKmh = 8f))
                evaluate()
                fastForward()
            }
            DemoEvent.STRAIGHT_15 -> {
                vehicle.override(VehicleSnapshot(Gear.DRIVE, speedKmh = 60f))
                fakeNavi.setStraightSince(clock.now())
                fastForward()
            }
            DemoEvent.COMPLEX_START -> fakeNavi.setComplexSection(true)
            DemoEvent.COMPLEX_END -> fakeNavi.setComplexSection(false)
            DemoEvent.ARRIVAL_10 -> fakeNavi.setMinutesToArrival(9)
            DemoEvent.PARK -> Unit
        }
    }

    /** 시연: 15분이 지난 것처럼 시계를 당기고, 바로 다시 제안할 수 있게 한다. */
    private fun fastForward() {
        clock.advance(TriggerConfig().dwellMs)
        engine.clearCooldown()
        evaluate()
    }

    private companion object {
        const val TAG = "Situation"
        const val TICK_MS = 5_000L
    }
}
