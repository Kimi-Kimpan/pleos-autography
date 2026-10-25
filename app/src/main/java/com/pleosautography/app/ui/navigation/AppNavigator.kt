package com.pleosautography.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.pleosautography.app.domain.DriveMode

/** 화면이 2단 배치에서 어디에 놓이는지. */
enum class PaneRole {
    /** 홈. 2단이면 왼쪽 창에 늘 떠 있다. */
    HOME,

    /** 홈에서 여는 화면. 2단이면 오른쪽 창에 열린다. */
    DETAIL,
}

/**
 * docs/screens.md의 화면. 06(NavToolbar)은 내비 옆 세로 창에 뜬 Driving이라 따로 두지 않는다.
 * @param driving 주행 중에도 열 수 있는 화면인지(정차 중에는 모든 화면을 연다).
 * @param drivingOnly 주행 중에만 의미가 있는 화면인지. 정차하면 닫는다.
 */
enum class Screen(
    val label: String,
    val role: PaneRole = PaneRole.DETAIL,
    val driving: Boolean = false,
    val drivingOnly: Boolean = false,
) {
    Main("01 홈 · 챕터 목록", PaneRole.HOME),
    Driving("02 주행 중 홈", PaneRole.HOME, driving = true, drivingOnly = true),
    Player("03 오토그래피 라디오"),
    Interview("04 음성 인터뷰", driving = true),
    Script("05 원고 보기"),
    Nudge("07 정체 감지 제안", driving = true, drivingOnly = true),
    Pause("08 도착 전 마무리", driving = true, drivingOnly = true),
    ListenDrive("09 원고 듣기·고치기", driving = true),
    Complete("10 챕터 완성"),
    Summary("10-3 인터뷰 요약", driving = true),
    Onboarding("11-1 첫 실행 · 부르는 이름"),
    Onboarding2("11-2 자서전 제목"),
    Onboarding3("11-3 첫 챕터"),
    Settings("12 환경설정"),
}

/** 단일 Activity의 화면 스택. 마지막 항목이 지금 화면이다. */
class AppNavigator(initial: List<Screen>) {
    val backStack = mutableStateListOf<Screen>().apply { addAll(initial) }

    val current: Screen get() = backStack.last()

    val canGoBack: Boolean get() = backStack.size > 1

    /** 홈 화면으로 가면 스택을 비우고, 그 밖의 화면은 위에 쌓는다. */
    fun navigate(screen: Screen) {
        if (screen == current) return
        if (screen.role == PaneRole.HOME) backStack.clear()
        backStack.add(screen)
    }

    /** Main에서 화면을 연다. 이미 열린 화면이 있으면 바꿔 끼운다(가로 전체 창 오른쪽 칸이 하나뿐이므로). */
    fun openFromMain(screen: Screen) {
        backStack.clear()
        backStack.add(Screen.Main)
        backStack.add(screen)
    }

    /** 스택을 [screen]의 홈 위에 [screen] 하나만 있게 바꾼다(디버그 실행, 상태 전환용). */
    fun reset(screen: Screen) {
        backStack.clear()
        if (screen.role != PaneRole.HOME) backStack.add(if (screen.driving) Screen.Driving else Screen.Main)
        backStack.add(screen)
    }

    /**
     * 지금 화면이 주행 상태와 맞지 않으면 그 상태의 홈으로 보낸다.
     * 주행 중에는 주행 중에 열 수 있는 화면만 두고(출발 → 주행 홈),
     * 정차하면 주행 전용 화면(주행 홈·제안·마무리)을 닫는다(기어 P → Main). 인터뷰는 정차 중에도 이어 갈 수 있다.
     */
    fun syncWithDriveMode(mode: DriveMode) {
        when {
            mode == DriveMode.DRIVING && !current.driving -> reset(Screen.Driving)
            mode == DriveMode.PARKED && current.drivingOnly -> reset(Screen.Main)
        }
    }

    /** 인터뷰를 끝내고 요약 화면으로: 정차 중 시작했으면 Main 위, 주행 중이면 주행 홈 위에 요약 하나만. */
    fun showSummary() {
        val home = if (backStack.contains(Screen.Main)) Screen.Main else Screen.Driving
        backStack.clear()
        backStack.add(home)
        backStack.add(Screen.Summary)
    }

    fun back() {
        if (canGoBack) backStack.removeAt(backStack.lastIndex)
    }

    companion object {
        val Saver = listSaver<AppNavigator, String>(
            save = { navigator -> navigator.backStack.map { it.name } },
            restore = { names -> AppNavigator(names.map(Screen::valueOf)) },
        )
    }
}

/** @param start 처음 열 화면(디버그 실행). 저장된 상태가 있으면 무시한다. */
@Composable
fun rememberAppNavigator(start: Screen? = null): AppNavigator =
    rememberSaveable(saver = AppNavigator.Saver) {
        AppNavigator(listOf(Screen.Main)).apply { start?.let(::reset) }
    }
