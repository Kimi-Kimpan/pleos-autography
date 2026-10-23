package com.pleosautography.app.domain

import com.pleosautography.app.data.vehicle.Gear
import com.pleosautography.app.data.vehicle.VehicleSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class DriveModeRuleTest {

    private fun mode(gear: Gear, speed: Float? = null, ux: Boolean? = null) =
        DriveModeRule.resolve(VehicleSnapshot(gear, speed, ux))

    @Test
    fun `P단이고 멈춰 있으면 정차`() {
        assertEquals(DriveMode.PARKED, mode(Gear.PARK, 0f))
        assertEquals(DriveMode.PARKED, mode(Gear.PARK, 0.3f))
    }

    @Test
    fun `속도를 모르면 P단만으로 정차`() {
        assertEquals(DriveMode.PARKED, mode(Gear.PARK, null))
    }

    @Test
    fun `D·R단이면 멈춰 있어도 주행`() {
        assertEquals(DriveMode.DRIVING, mode(Gear.DRIVE, 0f))
        assertEquals(DriveMode.DRIVING, mode(Gear.REVERSE, 0f))
    }

    @Test
    fun `N단이나 기어를 모르면 주행으로 본다`() {
        assertEquals(DriveMode.DRIVING, mode(Gear.NEUTRAL, 0f))
        assertEquals(DriveMode.DRIVING, mode(Gear.UNKNOWN))
    }

    @Test
    fun `P단이어도 움직이면 주행`() {
        assertEquals(DriveMode.DRIVING, mode(Gear.PARK, 3f))
    }

    @Test
    fun `차량이 주행 제한을 요구하면 주행`() {
        assertEquals(DriveMode.DRIVING, mode(Gear.PARK, 0f, ux = true))
        assertEquals(DriveMode.PARKED, mode(Gear.PARK, 0f, ux = false))
    }
}
