package com.pleosautography.app.sdkcheck

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import android.car.VehicleGear as CarGear

/**
 * 마일스톤 0: 차량 SDK 연결 확인용 화면(디버그 빌드 전용).
 *
 * - Pleos Vehicle SDK: Vehicle(context) → initialize → 기어 조회·구독 → onDestroy에서 release
 * - Android Car API: 기어(GEAR_SELECTION)와 속도(PERF_VEHICLE_SPEED)
 *   Pleos Vehicle SDK 2.3.2에는 속도 API가 없고, 기어 API는 deprecated(Google GEAR_SELECTION 사용 권장)다.
 */
class VehicleCheckActivity : ComponentActivity() {

    private lateinit var vehicle: Vehicle
    private var car: Car? = null
    private var propertyManager: CarPropertyManager? = null

    private lateinit var pleosGearText: TextView
    private lateinit var carGearText: TextView
    private lateinit var speedText: TextView
    private lateinit var modeText: TextView
    private lateinit var logText: TextView

    private var carGear: Int? = null
    private var speedMps: Float? = null

    private val requestSpeedPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) subscribeSpeed() else log("CAR_SPEED 권한 거부 — 속도를 읽을 수 없음")
        }

    private val gearListener = object : GearSelectionListener {
        override fun onSelected(gear: VehicleGear?) = runOnUiThread {
            pleosGearText.text = "Pleos 기어: ${gear?.name ?: "null"}"
            log("Pleos 기어 이벤트: ${gear?.name}")
        }

        override fun onFailed(e: Exception) = runOnUiThread {
            log("Pleos 기어 구독 실패: ${e.message}")
        }
    }

    private val carPropertyCallback = object : CarPropertyManager.CarPropertyEventCallback {
        // status 대신 쓰는 propertyStatus는 API 35 이후에만 있다.
        @Suppress("DEPRECATION")
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            if (value.status != CarPropertyValue.STATUS_AVAILABLE) {
                runOnUiThread { log("${VehiclePropertyIds.toString(value.propertyId)} 상태: ${value.status}") }
                return
            }
            when (value.propertyId) {
                VehiclePropertyIds.GEAR_SELECTION -> carGear = value.value as Int
                VehiclePropertyIds.PERF_VEHICLE_SPEED -> speedMps = value.value as Float
            }
            runOnUiThread { render() }
        }

        override fun onErrorEvent(propertyId: Int, areaId: Int) {
            runOnUiThread { log("Car API 오류: ${VehiclePropertyIds.toString(propertyId)}") }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContentView())

        // Pleos 가이드의 첫 번째 API 호출 흐름
        vehicle = Vehicle(this)
        vehicle.initialize()
        readPleosGear()

        connectCarApi()
        render()
    }

    @Suppress("DEPRECATION")
    override fun onDestroy() {
        runCatching { vehicle.getGear().unregisterGearSelection(gearListener) }
        vehicle.release()

        propertyManager?.unregisterCallback(carPropertyCallback)
        car?.disconnect()
        super.onDestroy()
    }

    // SDK 2.3.2에서 deprecated지만 연결 확인용으로 그대로 호출한다.
    @Suppress("DEPRECATION")
    private fun readPleosGear() {
        val gear = vehicle.getGear()
        gear.getCurrentGearSelectionState(
            onComplete = { value ->
                runOnUiThread {
                    pleosGearText.text = "Pleos 기어: ${value?.name ?: "null"}"
                    log("Pleos 기어 조회: ${value?.name}")
                }
            },
            onFailed = { e -> runOnUiThread { log("Pleos 기어 조회 실패: ${e.message}") } },
        )
        try {
            gear.registerGearSelection(gearListener)
        } catch (e: SecurityException) {
            log("Pleos 기어 구독 권한 없음: ${e.message}")
        }
    }

    private fun connectCarApi() {
        val car = Car.createCar(this)
        if (car == null) {
            log("Car 서비스에 연결하지 못함")
            return
        }
        this.car = car
        propertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager

        subscribe(VehiclePropertyIds.GEAR_SELECTION, CarPropertyManager.SENSOR_RATE_ONCHANGE)
        if (checkSelfPermission(Car.PERMISSION_SPEED) == PackageManager.PERMISSION_GRANTED) {
            subscribeSpeed()
        } else {
            requestSpeedPermission.launch(Car.PERMISSION_SPEED)
        }
    }

    private fun subscribeSpeed() {
        subscribe(VehiclePropertyIds.PERF_VEHICLE_SPEED, CarPropertyManager.SENSOR_RATE_NORMAL)
    }

    // API 34 에뮬레이터에서도 동작하도록 registerCallback을 쓴다(subscribePropertyEvents는 이후 버전).
    @Suppress("DEPRECATION")
    private fun subscribe(propertyId: Int, rate: Float) {
        val name = VehiclePropertyIds.toString(propertyId)
        try {
            val ok = propertyManager?.registerCallback(carPropertyCallback, propertyId, rate) ?: false
            log("$name 구독: ${if (ok) "성공" else "실패(미지원 속성?)"}")
        } catch (e: SecurityException) {
            log("$name 구독 권한 없음: ${e.message}")
        }
    }

    private fun render() {
        carGearText.text = "Car API 기어: ${carGear?.let(::gearName) ?: "—"}"
        speedText.text = "속도: ${speedMps?.let { "%.1f km/h".format(it * 3.6f) } ?: "—"}"
        // 정차는 P단 + 속도 0일 때만. 그 밖에는 모두 주행(제한 모드)으로 본다.
        modeText.text = when {
            carGear == null -> "판정: 값 대기 중"
            carGear == CarGear.GEAR_PARK && (speedMps ?: 0f) == 0f -> "판정: 정차 (P)"
            else -> "판정: 주행 중"
        }
    }

    private fun gearName(gear: Int): String = when (gear) {
        CarGear.GEAR_PARK -> "P"
        CarGear.GEAR_REVERSE -> "R"
        CarGear.GEAR_NEUTRAL -> "N"
        CarGear.GEAR_DRIVE -> "D"
        else -> CarGear.toString(gear)
    }

    private fun log(message: String) {
        Log.i(TAG, message)
        logText.append("$message\n")
    }

    private fun buildContentView(): LinearLayout {
        fun label(sizeSp: Float) = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
            setPadding(0, 0, 0, 24)
        }
        pleosGearText = label(32f).apply { text = "Pleos 기어: —" }
        carGearText = label(32f)
        speedText = label(32f)
        modeText = label(40f)
        logText = label(18f)
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            fitsSystemWindows = true
            setPadding(64, 64, 64, 64)
            addView(label(22f).apply { text = "마일스톤 0 · 차량 SDK 연결 확인" })
            addView(pleosGearText)
            addView(carGearText)
            addView(speedText)
            addView(modeText)
            addView(logText)
        }
    }

    private companion object {
        const val TAG = "VehicleCheck"
    }
}
