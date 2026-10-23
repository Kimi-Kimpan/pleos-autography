package com.pleosautography.app.data.vehicle

import android.car.Car
import android.car.VehicleGear
import android.car.VehiclePropertyIds
import android.car.drivingstate.CarUxRestrictionsManager
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Android Car API로 기어(GEAR_SELECTION)·속도(PERF_VEHICLE_SPEED)·주행 제한(CarUxRestrictions)을 읽는다.
 * 속도는 런타임 권한(CAR_SPEED)이 있어야 읽으므로, 권한을 받은 뒤 [onSpeedPermissionGranted]를 부른다.
 * 앱 프로세스 동안 연결을 유지한다.
 */
class CarApiVehicleState(private val context: Context) : VehicleState {

    private val state = MutableStateFlow(VehicleSnapshot())
    override val snapshot: StateFlow<VehicleSnapshot> = state.asStateFlow()

    private var propertyManager: CarPropertyManager? = null
    private var speedSubscribed = false

    private val callback = object : CarPropertyManager.CarPropertyEventCallback {
        // propertyStatus는 API 35 이후에만 있다.
        @Suppress("DEPRECATION")
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            if (value.status != CarPropertyValue.STATUS_AVAILABLE) return
            when (value.propertyId) {
                VehiclePropertyIds.GEAR_SELECTION -> state.update { it.copy(gear = toGear(value.value as Int)) }
                VehiclePropertyIds.PERF_VEHICLE_SPEED -> state.update { it.copy(speedKmh = (value.value as Float) * 3.6f) }
            }
        }

        override fun onErrorEvent(propertyId: Int, areaId: Int) {
            Log.w(TAG, "Car API 오류: ${VehiclePropertyIds.toString(propertyId)}")
        }
    }

    /** Car 서비스에 연결하고 구독을 시작한다. 차량 서비스가 없으면 값은 UNKNOWN으로 남는다. */
    fun start() {
        val car = runCatching { Car.createCar(context) }.getOrNull()
        if (car == null) {
            Log.w(TAG, "Car 서비스에 연결하지 못함")
            return
        }
        propertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
        subscribe(VehiclePropertyIds.GEAR_SELECTION, CarPropertyManager.SENSOR_RATE_ONCHANGE)
        onSpeedPermissionGranted()

        val ux = car.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager
        ux?.let { manager ->
            state.update { it.copy(uxRestricted = manager.currentCarUxRestrictions?.isRequiresDistractionOptimization) }
            manager.registerListener { restrictions ->
                state.update { it.copy(uxRestricted = restrictions.isRequiresDistractionOptimization) }
            }
        }
    }

    /** 속도 권한이 있으면 속도 구독을 시작한다(여러 번 불러도 한 번만 구독). */
    fun onSpeedPermissionGranted() {
        if (speedSubscribed || context.checkSelfPermission(Car.PERMISSION_SPEED) != PackageManager.PERMISSION_GRANTED) return
        speedSubscribed = subscribe(VehiclePropertyIds.PERF_VEHICLE_SPEED, CarPropertyManager.SENSOR_RATE_NORMAL)
    }

    // API 34 에뮬레이터에서도 동작하도록 registerCallback을 쓴다(subscribePropertyEvents는 이후 버전).
    @Suppress("DEPRECATION")
    private fun subscribe(propertyId: Int, rate: Float): Boolean = try {
        propertyManager?.registerCallback(callback, propertyId, rate) ?: false
    } catch (e: SecurityException) {
        Log.w(TAG, "${VehiclePropertyIds.toString(propertyId)} 권한 없음", e)
        false
    }

    private fun toGear(value: Int): Gear = when (value) {
        VehicleGear.GEAR_PARK -> Gear.PARK
        VehicleGear.GEAR_REVERSE -> Gear.REVERSE
        VehicleGear.GEAR_NEUTRAL -> Gear.NEUTRAL
        VehicleGear.GEAR_UNKNOWN -> Gear.UNKNOWN
        else -> Gear.DRIVE
    }

    private companion object {
        const val TAG = "CarApiVehicleState"
    }
}
