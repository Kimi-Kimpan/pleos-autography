package com.pleosautography.app.data.vehicle

import kotlinx.coroutines.flow.StateFlow

/** 앱이 쓰는 기어 구분. 1~9단 기어는 [DRIVE]로 본다. */
enum class Gear { PARK, REVERSE, NEUTRAL, DRIVE, UNKNOWN }

/**
 * 차량 상태 한 순간.
 * @param speedKmh 속도 권한이 없거나 아직 값이 없으면 null.
 * @param uxRestricted CarUxRestrictions가 주행 중 제한(DO 필요)을 요구하는지. 모르면 null.
 */
data class VehicleSnapshot(
    val gear: Gear = Gear.UNKNOWN,
    val speedKmh: Float? = null,
    val uxRestricted: Boolean? = null,
)

/**
 * 차량 상태 어댑터(CLAUDE.md의 SDK 어댑터 패턴).
 * - [CarApiVehicleState]: Android Car API(Pleos Connect에서 동작). Vehicle SDK 2.3.2에는 속도 API가 없다.
 * - [DemoVehicleState]: 시연·에뮬레이터용으로 값을 덮어쓴다.
 */
interface VehicleState {
    val snapshot: StateFlow<VehicleSnapshot>
}
