package com.pleosautography.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pleosautography.app.data.demo.DemoData
import com.pleosautography.app.domain.DriveMode
import com.pleosautography.app.domain.demo.DemoEvent
import com.pleosautography.app.ui.components.Spine
import com.pleosautography.app.ui.components.VLine
import com.pleosautography.app.ui.debug.DemoPanel
import com.pleosautography.app.ui.navigation.AppNavigator
import com.pleosautography.app.ui.navigation.PaneRole
import com.pleosautography.app.ui.navigation.Screen
import com.pleosautography.app.ui.navigation.rememberAppNavigator
import com.pleosautography.app.ui.screens.EmptyDetailPane
import com.pleosautography.app.ui.screens.PlaceholderScreen
import com.pleosautography.app.ui.screens.drive.DrivingScreen
import com.pleosautography.app.ui.screens.drive.InterviewScreen
import com.pleosautography.app.ui.screens.drive.MyBookSidePane
import com.pleosautography.app.ui.screens.drive.NudgeScreen
import com.pleosautography.app.ui.screens.drive.PauseScreen
import com.pleosautography.app.ui.screens.drive.RadioSidePane
import com.pleosautography.app.ui.screens.main.MainScreen
import com.pleosautography.app.ui.theme.AutoColors
import com.pleosautography.app.ui.theme.AutographyTheme
import com.pleosautography.app.ui.theme.DemoOptions
import com.pleosautography.app.ui.theme.DesignFrame
import com.pleosautography.app.ui.theme.px

/**
 * 창 폭(시안 px)이 이 값 이상이면 2단으로 배치한다.
 * Pleos 앱 영역의 세로 한쪽 창은 약 890px, 가로 전체 창은 약 1780px이다.
 */
private const val TWO_PANE_MIN_WIDTH = 1400f

/** 가로 전체 창 주행 중 왼쪽 “말하는 화면”의 폭(시안 px, design/html/FullDrive.html). */
private const val DRIVE_TALK_PANE_WIDTH = 888f

/**
 * 디버그 빌드에서 adb로 연 화면과 시연 설정. [id]가 바뀔 때마다 다시 적용한다.
 */
data class LaunchRequest(val id: Long = 0, val screen: Screen? = null, val demoOptions: DemoOptions = DemoOptions())

/**
 * 앱의 뼈대: 책등 + 화면.
 * - 시안 1px = 창 높이 / 1280 dp.
 * - 세로 한쪽 창: 지금 화면 하나.
 * - 가로 전체 창, 정차: 왼쪽 Main, 오른쪽 Main에서 연 화면.
 * - 가로 전체 창, 주행: 왼쪽은 마이크를 쓰는 화면 하나(주행 홈·제안·인터뷰), 오른쪽은 듣기·보기 전용 칸.
 *   인터뷰·원고 듣기 중이면 내 자서전, 그 밖에는 오토그래피 라디오. 마이크 화면이 두 개 열리지 않게 한다.
 * - 도착 전 마무리(Pause)는 책등 없이 창 전체를 덮는다.
 *
 * @param driveMode 차량 상태로 정한 주행/정차(AppGraph.driveMode).
 * @param onDemoEvent 디버그 빌드에서만 준다. 책등의 ‘Autography’를 길게 누르면 데모 패널이 열린다.
 */
@Composable
fun AutographyApp(
    driveMode: DriveMode,
    launch: LaunchRequest = LaunchRequest(),
    onDemoEvent: ((DemoEvent) -> Unit)? = null,
) {
    // 시작 화면은 처음 스택에 바로 넣는다. 창 크기 변경으로 Activity가 다시 만들어져도 저장된 스택을 쓴다.
    val navigator = rememberAppNavigator(launch.screen)
    BackHandler(enabled = navigator.canGoBack) { navigator.back() }
    LaunchedEffect(launch.id) { launch.screen?.let(navigator::reset) }
    // 주행 상태가 바뀌거나 맞지 않는 화면이 열리면 그 상태의 홈으로 보낸다.
    LaunchedEffect(driveMode, navigator.current) { navigator.syncWithDriveMode(driveMode) }
    var demoPanelOpen by remember { mutableStateOf(false) }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(AutoColors.Bg)
            .safeDrawingPadding(),
    ) {
        val scale = maxHeight.value / DesignFrame.HEIGHT
        val twoPane = maxWidth.value / scale >= TWO_PANE_MIN_WIDTH

        AutographyTheme(designScale = scale, driveMode = driveMode, demoOptions = launch.demoOptions) {
            if (navigator.current == Screen.Pause) {
                ScreenContent(Screen.Pause, navigator, Modifier.fillMaxSize())
            } else {
                AppPanes(
                    navigator = navigator,
                    driveMode = driveMode,
                    twoPane = twoPane,
                    onLogoLongPress = onDemoEvent?.let { { demoPanelOpen = !demoPanelOpen } },
                )
            }
            if (onDemoEvent != null && demoPanelOpen) {
                DemoPanel(
                    onEvent = onDemoEvent,
                    onClose = { demoPanelOpen = false },
                    modifier = Modifier.align(Alignment.BottomStart),
                )
            }
        }
    }
}

@Composable
private fun AppPanes(
    navigator: AppNavigator,
    driveMode: DriveMode,
    twoPane: Boolean,
    onLogoLongPress: (() -> Unit)?,
) {
    Row(Modifier.fillMaxSize()) {
        Spine(
            bookTitle = DemoData.BOOK_TITLE,
            onSettings = { navigator.navigate(Screen.Settings) },
            onLogoLongPress = onLogoLongPress,
        )
        if (twoPane && driveMode == DriveMode.DRIVING) {
            val talking = navigator.current.takeIf { it.driving } ?: Screen.Driving
            ScreenContent(talking, navigator, Modifier.width(DRIVE_TALK_PANE_WIDTH.px), fullWindow = true)
            VLine(color = AutoColors.Ink)
            if (talking == Screen.Interview || talking == Screen.ListenDrive) {
                MyBookSidePane(Modifier.weight(1f))
            } else {
                RadioSidePane(Modifier.weight(1f))
            }
        } else if (twoPane) {
            val detail = navigator.current.takeIf { it.role == PaneRole.DETAIL }
            ScreenContent(Screen.Main, navigator, Modifier.weight(1f))
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

@Composable
private fun ScreenContent(
    screen: Screen,
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
    fullWindow: Boolean = false,
) {
    when (screen) {
        Screen.Main -> MainScreen(
            onContinueWriting = { navigator.navigate(Screen.Interview) },
            onListen = { navigator.navigate(Screen.Player) },
            onOpenScript = { navigator.navigate(Screen.Script) },
            onOpenPlayer = { navigator.navigate(Screen.Player) },
            modifier = modifier,
        )
        Screen.Driving -> DrivingScreen(
            onCallOto = { navigator.navigate(Screen.Interview) },
            onOpenPlayer = { navigator.navigate(Screen.Player) },
            modifier = modifier,
            showMiniPlayer = !fullWindow,
        )
        Screen.Interview -> InterviewScreen(
            // 정차 중에 시작한 인터뷰라면 Main으로, 주행 중이면 주행 홈으로 돌아간다.
            onStopHere = { navigator.navigate(if (navigator.backStack.contains(Screen.Main)) Screen.Main else Screen.Driving) },
            modifier = modifier,
        )
        Screen.Nudge -> NudgeScreen(
            onAccept = { navigator.reset(Screen.Interview) },
            onLater = { navigator.navigate(Screen.Driving) },
            modifier = modifier,
        )
        Screen.Pause -> PauseScreen(
            onSaveAndStop = { navigator.navigate(Screen.Driving) },
            onContinue = { navigator.reset(Screen.Interview) },
            modifier = modifier,
        )
        else -> PlaceholderScreen(
            screen = screen,
            onBack = if (screen.role == PaneRole.DETAIL) navigator::back else null,
            modifier = modifier,
        )
    }
}
