package com.pleosautography.app.domain

import com.pleosautography.app.data.vehicle.Gear
import com.pleosautography.app.data.vehicle.VehicleSnapshot

/** 주행/정차 상태. */
enum class DriveMode { PARKED, DRIVING }

/**
 * 주행/정차 판정(CLAUDE.md 핵심 규칙 1·2).
 * - 정차: P단이고 멈춰 있을 때만. 속도를 모르면(권한 없음) P단만으로 정차로 본다.
 * - 주행: 그 밖의 모든 경우. 기어를 아직 모르면 안전하게 주행으로 본다.
 * - 차량이 주행 중 제한(CarUxRestrictions)을 요구하면 기어와 상관없이 주행이다.
 */
object DriveModeRule {
    /** 이 속도 이하를 멈춘 것으로 본다(센서 잡음 대비). */
    const val STOPPED_SPEED_KMH = 0.5f

    fun resolve(snapshot: VehicleSnapshot): DriveMode {
        if (snapshot.uxRestricted == true) return DriveMode.DRIVING
        val stopped = (snapshot.speedKmh ?: 0f) <= STOPPED_SPEED_KMH
        return if (snapshot.gear == Gear.PARK && stopped) DriveMode.PARKED else DriveMode.DRIVING
    }
}
