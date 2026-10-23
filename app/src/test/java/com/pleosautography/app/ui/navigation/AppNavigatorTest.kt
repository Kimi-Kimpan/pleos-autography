package com.pleosautography.app.ui.navigation

import com.pleosautography.app.domain.DriveMode
import org.junit.Assert.assertEquals
import org.junit.Test

class AppNavigatorTest {

    @Test
    fun `출발하면 정차 화면을 닫고 주행 홈으로 간다`() {
        val navigator = AppNavigator(listOf(Screen.Main, Screen.Script))
        navigator.syncWithDriveMode(DriveMode.DRIVING)
        assertEquals(listOf(Screen.Driving), navigator.backStack.toList())
    }

    @Test
    fun `주행 중 열 수 있는 화면은 그대로 둔다`() {
        val navigator = AppNavigator(listOf(Screen.Driving, Screen.Interview))
        navigator.syncWithDriveMode(DriveMode.DRIVING)
        assertEquals(Screen.Interview, navigator.current)
    }

    @Test
    fun `P단이 되면 주행 전용 화면을 닫고 Main으로 간다`() {
        for (screen in listOf(Screen.Driving, Screen.Nudge, Screen.Pause)) {
            val navigator = AppNavigator(listOf(Screen.Driving)).apply { navigate(screen) }
            navigator.syncWithDriveMode(DriveMode.PARKED)
            assertEquals(listOf(Screen.Main), navigator.backStack.toList())
        }
    }

    @Test
    fun `정차 중 인터뷰는 이어 갈 수 있다`() {
        val navigator = AppNavigator(listOf(Screen.Main, Screen.Interview))
        navigator.syncWithDriveMode(DriveMode.PARKED)
        assertEquals(Screen.Interview, navigator.current)
    }

    @Test
    fun `홈으로 가면 스택을 비운다`() {
        val navigator = AppNavigator(listOf(Screen.Driving, Screen.Interview))
        navigator.navigate(Screen.Driving)
        assertEquals(listOf(Screen.Driving), navigator.backStack.toList())
    }
}
