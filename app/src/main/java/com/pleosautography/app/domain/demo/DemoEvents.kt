package com.pleosautography.app.domain.demo

import android.util.Log
import com.pleosautography.app.data.vehicle.DemoVehicleState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 시연 상황을 버튼 하나(또는 adb 한 줄)로 재현하는 데모 이벤트.
 * adb 이름은 [id]다: `adb shell am broadcast -a com.pleosautography.app.DEMO_EVENT -p com.pleosautography.app --es event drive`
 */
enum class DemoEvent(val id: String, val label: String) {
    DRIVE("drive", "주행 (D · 40km/h)"),
    PARK("park", "정차 (P)"),
    REAL_VEHICLE("real", "실제 차량 값"),

    // 마일스톤 6(상황 트리거)에서 처리한다.
    CONGESTION_15("congestion15", "정체 15분"),
    STRAIGHT_15("straight15", "긴 직진 15분"),
    COMPLEX_START("complex_start", "복잡 구간 시작"),
    COMPLEX_END("complex_end", "복잡 구간 끝"),
    ARRIVAL_10("arrival10", "도착 10분 전"),
    ;

    companion object {
        fun fromId(id: String?): DemoEvent? = entries.firstOrNull { it.id == id }
    }
}

/** 데모 이벤트를 받아 Fake 상태에 반영한다. 차량 상태 밖의 이벤트는 [events]로 흘려보낸다. */
class DemoEvents(private val vehicle: DemoVehicleState) {
    private val flow = MutableSharedFlow<DemoEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<DemoEvent> = flow.asSharedFlow()

    fun send(event: DemoEvent) {
        Log.i(TAG, "데모 이벤트: ${event.id}")
        when (event) {
            DemoEvent.DRIVE -> vehicle.override(DemoVehicleState.Driving)
            DemoEvent.PARK -> vehicle.override(DemoVehicleState.Parked)
            DemoEvent.REAL_VEHICLE -> vehicle.override(null)
            else -> Unit
        }
        flow.tryEmit(event)
    }

    private companion object {
        const val TAG = "DemoEvents"
    }
}
