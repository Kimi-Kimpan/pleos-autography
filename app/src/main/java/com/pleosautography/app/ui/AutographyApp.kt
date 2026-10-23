package com.pleosautography.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pleosautography.app.data.demo.DemoData
import com.pleosautography.app.ui.components.Spine
import com.pleosautography.app.ui.components.VLine
import com.pleosautography.app.ui.navigation.AppNavigator
import com.pleosautography.app.ui.navigation.PaneRole
import com.pleosautography.app.ui.navigation.Screen
import com.pleosautography.app.ui.navigation.rememberAppNavigator
import com.pleosautography.app.ui.screens.EmptyDetailPane
import com.pleosautography.app.ui.screens.PlaceholderScreen
import com.pleosautography.app.ui.screens.main.MainScreen
import com.pleosautography.app.ui.theme.AutoColors
import com.pleosautography.app.ui.theme.AutographyTheme
import com.pleosautography.app.ui.theme.DesignFrame
import com.pleosautography.app.ui.theme.DriveMode
import com.pleosautography.app.ui.theme.LocalDriveMode

/**
 * 창 폭(시안 px)이 이 값 이상이면 2단으로 배치한다.
 * Pleos 앱 영역의 세로 한쪽 창은 약 890px, 가로 전체 창은 약 1780px이다.
 */
private const val TWO_PANE_MIN_WIDTH = 1400f

/**
 * 앱의 뼈대: 책등 + 화면.
 * - 시안 1px = 창 높이 / 1280 dp.
 * - 세로 한쪽 창: 지금 화면 하나.
 * - 가로 전체 창: 왼쪽은 홈(정차 Main / 주행 Driving), 오른쪽은 홈에서 연 화면.
 */
@Composable
fun AutographyApp() {
    val navigator = rememberAppNavigator()
    BackHandler(enabled = navigator.canGoBack) { navigator.back() }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(AutoColors.Bg)
            .safeDrawingPadding(),
    ) {
        val scale = maxHeight.value / DesignFrame.HEIGHT
        val twoPane = maxWidth.value / scale >= TWO_PANE_MIN_WIDTH

        AutographyTheme(designScale = scale) {
            Row(Modifier.fillMaxSize()) {
                Spine(DemoData.BOOK_TITLE, onSettings = { navigator.navigate(Screen.Settings) })
                if (twoPane) {
                    val home = if (LocalDriveMode.current == DriveMode.PARKED) Screen.Main else Screen.Driving
                    val detail = navigator.current.takeIf { it.role == PaneRole.DETAIL }
                    ScreenContent(home, navigator, Modifier.weight(1f))
                    VLine(color = AutoColors.Ink)
                    if (detail != null) {
                        ScreenContent(detail, navigator, Modifier.weight(1f))
                    } else {
                        EmptyDetailPane(Modifier.weight(1f))
                    }
                } else {
                    ScreenContent(navigator.current, navigator, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ScreenContent(screen: Screen, navigator: AppNavigator, modifier: Modifier = Modifier) {
    when (screen) {
        Screen.Main -> MainScreen(
            onContinueWriting = { navigator.navigate(Screen.Interview) },
            onListen = { navigator.navigate(Screen.Player) },
            onOpenScript = { navigator.navigate(Screen.Script) },
            onOpenPlayer = { navigator.navigate(Screen.Player) },
            modifier = modifier,
        )
        else -> PlaceholderScreen(
            screen = screen,
            onBack = if (screen.role == PaneRole.DETAIL) navigator::back else null,
            modifier = modifier,
        )
    }
}
