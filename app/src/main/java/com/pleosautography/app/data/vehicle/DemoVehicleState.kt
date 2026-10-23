package com.pleosautography.app.data.vehicle

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 시연용 차량 상태(Fake). 평소에는 [real] 값을 그대로 내보내고,
 * [override]로 값을 주면 그 값을 우선한다. 데모 이벤트(주행/정차)와 디버그 실행이 쓴다.
 * 덮어쓰기는 바로(동기로) 반영되어, 같은 순간에 연 화면과 주행 상태가 어긋나지 않는다.
 */
class DemoVehicleState(private val real: VehicleState, scope: CoroutineScope) : VehicleState {

    private val state = MutableStateFlow(real.snapshot.value)
    override val snapshot: StateFlow<VehicleSnapshot> = state.asStateFlow()

    /** 지금 덮어쓰고 있는 값. null이면 실제 차량 값을 쓴다. */
    var demoSnapshot: VehicleSnapshot? = null
        private set

    init {
        scope.launch {
            real.snapshot.collect { value -> if (demoSnapshot == null) state.value = value }
        }
    }

    fun override(value: VehicleSnapshot?) {
        demoSnapshot = value
        state.value = value ?: real.snapshot.value
    }

    companion object {
        /** 데모 ‘주행’: D단 40km/h. */
        val Driving = VehicleSnapshot(gear = Gear.DRIVE, speedKmh = 40f)

        /** 데모 ‘정차’: P단 0km/h. */
        val Parked = VehicleSnapshot(gear = Gear.PARK, speedKmh = 0f)
    }
}
