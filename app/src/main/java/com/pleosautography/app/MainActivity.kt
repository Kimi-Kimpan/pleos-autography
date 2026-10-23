package com.pleosautography.app

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pleosautography.app.ui.AutographyApp
import com.pleosautography.app.ui.LaunchRequest
import com.pleosautography.app.ui.navigation.Screen
import com.pleosautography.app.ui.theme.DemoOptions

class MainActivity : ComponentActivity() {

    private var launch by mutableStateOf(LaunchRequest())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) launch = readLaunchRequest(intent)
        setContent { AutographyApp(launch) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        launch = readLaunchRequest(intent)
    }

    /**
     * 디버그 빌드 전용: adb로 화면과 시연 설정을 바로 연다.
     * 예) adb shell am start -n com.pleosautography.app/.MainActivity --es screen Nudge --ez timers false
     */
    private fun readLaunchRequest(intent: Intent): LaunchRequest {
        val debuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!debuggable) return LaunchRequest()
        val screen = intent.getStringExtra("screen")?.let { name -> Screen.entries.firstOrNull { it.name == name } }
        val options = DemoOptions(
            timersRunning = intent.getBooleanExtra("timers", true),
        )
        return LaunchRequest(id = System.nanoTime(), screen = screen, demoOptions = options)
    }
}
