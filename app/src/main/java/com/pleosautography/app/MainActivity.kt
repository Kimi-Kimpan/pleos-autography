package com.pleosautography.app

import android.car.Car
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pleosautography.app.domain.demo.DemoEvent
import com.pleosautography.app.ui.AutographyApp
import com.pleosautography.app.ui.LaunchRequest
import com.pleosautography.app.ui.navigation.Screen
import com.pleosautography.app.ui.theme.DemoOptions

class MainActivity : ComponentActivity() {

    private val graph by lazy { AppGraph.get(this) }
    private val debuggable by lazy { (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 }
    private var launch by mutableStateOf(LaunchRequest())

    private val requestSpeedPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) graph.carVehicle.onSpeedPermissionGranted()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) launch = readLaunchRequest(intent)
        if (checkSelfPermission(Car.PERMISSION_SPEED) != PackageManager.PERMISSION_GRANTED) {
            requestSpeedPermission.launch(Car.PERMISSION_SPEED)
        }
        setContent {
            val driveMode by graph.driveMode.collectAsState()
            AutographyApp(
                driveMode = driveMode,
                launch = launch,
                onDemoEvent = if (debuggable) graph.demoEvents::send else null,
                situation = graph.situation,
                playChime = graph.chime::play,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        launch = readLaunchRequest(intent)
    }

    /**
     * 디버그 빌드 전용: adb로 화면과 시연 설정을 바로 연다.
     * 예) adb shell am start -n com.pleosautography.app/.MainActivity --es screen Nudge --ez timers false
     * 화면을 주면 그 화면에 맞는 데모 주행 상태(주행 중에 여는 화면이면 D단, 아니면 P단)도 함께 넣는다.
     * `--es drive drive|park|real`로 따로 정할 수도 있다.
     */
    private fun readLaunchRequest(intent: Intent): LaunchRequest {
        if (!debuggable) return LaunchRequest()
        val screen = intent.getStringExtra("screen")?.let { name -> Screen.entries.firstOrNull { it.name == name } }
        val drive = DemoEvent.fromId(intent.getStringExtra("drive"))
            ?: screen?.let { if (it.driving) DemoEvent.DRIVE else DemoEvent.PARK }
        // 화면을 열기 전에 주행 상태부터 바꿔, 열자마자 홈으로 돌려보내지지 않게 한다.
        drive?.let(graph.demoEvents::send)
        val options = DemoOptions(timersRunning = intent.getBooleanExtra("timers", true))
        return LaunchRequest(id = System.nanoTime(), screen = screen, demoOptions = options)
    }
}
