package com.pleosautography.app

import android.content.Context
import com.pleosautography.app.data.audio.ChimePlayer
import com.pleosautography.app.data.navi.FakeNaviState
import com.pleosautography.app.data.vehicle.CarApiVehicleState
import com.pleosautography.app.data.vehicle.DemoVehicleState
import com.pleosautography.app.domain.DriveMode
import com.pleosautography.app.domain.DriveModeRule
import com.pleosautography.app.domain.demo.DemoEvents
import com.pleosautography.app.domain.trigger.SituationController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * 앱 전체에서 하나만 쓰는 객체들(간단한 수동 의존성 주입).
 * 차량 연결은 앱 프로세스 동안 유지한다.
 */
class AppGraph private constructor(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val carVehicle = CarApiVehicleState(context)
    val vehicle = DemoVehicleState(carVehicle, scope)
    val demoEvents = DemoEvents(vehicle)
    val chime = ChimePlayer()

    /** 경로 안내. Pleos NaviHelper 구현체는 마일스톤 8에서 붙이고, 그전에는 시연용 Fake를 쓴다. */
    val navi = FakeNaviState()
    val situation = SituationController(scope, vehicle, navi, demoEvents.events)

    /** 주행/정차. 시작값도 지금 차량 값으로 바로 계산해 화면이 한 번 잘못 바뀌지 않게 한다. */
    val driveMode: StateFlow<DriveMode> = vehicle.snapshot
        .map(DriveModeRule::resolve)
        .stateIn(scope, SharingStarted.Eagerly, DriveModeRule.resolve(vehicle.snapshot.value))

    init {
        carVehicle.start()
    }

    companion object {
        @Volatile private var instance: AppGraph? = null

        fun get(context: Context): AppGraph =
            instance ?: synchronized(this) {
                instance ?: AppGraph(context.applicationContext).also { instance = it }
            }
    }
}
