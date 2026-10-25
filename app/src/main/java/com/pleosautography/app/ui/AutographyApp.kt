package com.pleosautography.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Stable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pleosautography.app.data.audio.RadioPlayback
import com.pleosautography.app.data.audio.RadioState
import com.pleosautography.app.data.demo.DemoData
import kotlinx.coroutines.launch
import com.pleosautography.app.domain.DriveMode
import com.pleosautography.app.domain.demo.DemoEvent
import com.pleosautography.app.domain.interview.InterviewEvent
import com.pleosautography.app.domain.interview.InterviewSession
import com.pleosautography.app.domain.interview.InterviewUi
import com.pleosautography.app.domain.interview.VoiceCommand
import com.pleosautography.app.domain.trigger.NudgeReason
import com.pleosautography.app.domain.trigger.TriggerAction
import com.pleosautography.app.ui.components.Spine
import com.pleosautography.app.ui.components.StatusChip
import com.pleosautography.app.ui.components.VLine
import com.pleosautography.app.ui.debug.DemoPanel
import com.pleosautography.app.ui.navigation.AppNavigator
import com.pleosautography.app.ui.navigation.PaneRole
import com.pleosautography.app.ui.navigation.Screen
import com.pleosautography.app.ui.navigation.rememberAppNavigator
import com.pleosautography.app.ui.screens.BookState
import com.pleosautography.app.ui.screens.LocalBookState
import com.pleosautography.app.ui.screens.LocalInterviewUi
import com.pleosautography.app.ui.screens.LocalLiveProgress
import com.pleosautography.app.ui.screens.LocalRadioPlayback
import com.pleosautography.app.ui.screens.LocalRadioState
import com.pleosautography.app.ui.screens.onboarding.LocalOnboardingDraft
import com.pleosautography.app.ui.screens.onboarding.rememberOnboardingDraft
import com.pleosautography.app.ui.screens.PlaceholderScreen
import com.pleosautography.app.ui.screens.drive.ListenDriveScreen
import com.pleosautography.app.ui.screens.onboarding.OnboardingScreen
import com.pleosautography.app.ui.screens.player.PlayerScreen
import com.pleosautography.app.ui.screens.script.ScriptScreen
import com.pleosautography.app.ui.screens.settings.SettingsScreen
import com.pleosautography.app.ui.screens.drive.DriveSituation
import com.pleosautography.app.ui.screens.drive.DrivingScreen
import com.pleosautography.app.ui.screens.drive.ChapterFullActions
import com.pleosautography.app.ui.screens.drive.InterviewScreen
import com.pleosautography.app.ui.screens.drive.LocalDriveSituation
import com.pleosautography.app.ui.screens.drive.MyBookSidePane
import com.pleosautography.app.ui.screens.drive.NudgeScreen
import com.pleosautography.app.ui.screens.drive.PauseScreen
import com.pleosautography.app.ui.screens.drive.SummaryScreen
import com.pleosautography.app.ui.screens.drive.RadioSidePane
import com.pleosautography.app.ui.screens.main.ChapterAction
import com.pleosautography.app.ui.screens.main.CompleteScreen
import com.pleosautography.app.ui.screens.main.TOTAL_CHAPTERS
import com.pleosautography.app.ui.screens.main.MainScreen
import com.pleosautography.app.ui.screens.main.ParkedWelcomePane
import com.pleosautography.app.ui.theme.AutoColors
import com.pleosautography.app.ui.theme.AutographyTheme
import com.pleosautography.app.ui.theme.DemoOptions
import com.pleosautography.app.ui.theme.DesignFrame
import com.pleosautography.app.ui.theme.LocalDriveMode
import com.pleosautography.app.ui.theme.LocalShowStatusChip
import com.pleosautography.app.ui.theme.px

/**
 * 창 폭(시안 px)이 이 값 이상이면 2단으로 배치한다.
 * Pleos 앱 영역의 세로 한쪽 창은 약 890px, 가로 전체 창은 약 1780px이다.
 */
private const val TWO_PANE_MIN_WIDTH = 1400f

/** 가로 전체 창 정차 중 오른쪽 칸 위 여백(시안 px): 상태 칩(위 36 + 높이 약 36)과 겹치지 않게. */
private const val DETAIL_CHIP_INSET = 56f

/** 가로 전체 창 왼쪽 칸의 폭(시안 px, design/html/FullDrive.html·FullPark.html). */
private const val FULL_LEFT_PANE_WIDTH = 888f

/**
 * 디버그 빌드에서 adb로 연 화면과 시연 설정. [id]가 바뀔 때마다 다시 적용한다.
 */
data class LaunchRequest(val id: Long = 0, val screen: Screen? = null, val demoOptions: DemoOptions = DemoOptions())

/**
 * 앱의 뼈대: 책등 + 화면.
 * - 시안 1px = 창 높이 / 1280 dp.
 * - 세로 한쪽 창: 지금 화면 하나.
 * - 가로 전체 창, 정차: 왼쪽 챕터 목록(인사말 머리글 없이) + 미니 플레이어, 오른쪽은 고른 화면이나 인사말.
 * - 가로 전체 창, 주행: 왼쪽은 마이크를 쓰는 화면 하나(주행 홈·제안·인터뷰), 오른쪽은 듣기·보기 전용 칸.
 *   인터뷰·원고 듣기 중이면 내 자서전, 그 밖에는 오토그래피 라디오. 마이크 화면이 두 개 열리지 않게 한다.
 * - 도착 전 마무리(Pause)는 책등 없이 창 전체를 덮는다.
 * - 가로 전체 창에서 상태 칩은 창 오른쪽 위 한 곳에만 그린다(각 화면의 칩은 숨긴다).
 *
 * @param driveMode 차량 상태로 정한 주행/정차(AppGraph.driveMode).
 * @param onDemoEvent 디버그 빌드에서만 준다. 책등의 ‘Autography’를 길게 누르면 데모 패널이 열린다.
 * @param services 상황 트리거·인터뷰·음성 입력·라디오(AppGraph). 결정과 음성 명령에 따라 화면을 바꾼다.
 */
@Composable
fun AutographyApp(
    driveMode: DriveMode,
    launch: LaunchRequest = LaunchRequest(),
    onDemoEvent: ((DemoEvent) -> Unit)? = null,
    services: AppServices? = null,
) {
    // 시작 화면은 처음 스택에 바로 넣는다. 창 크기 변경으로 Activity가 다시 만들어져도 저장된 스택을 쓴다.
    val navigator = rememberAppNavigator(launch.screen)
    // 오토그래피 라디오 플레이어: 화면이 아니라 미니 플레이어가 있는 칸 위로 올라오는 시트다.
    val playerSheet = rememberSaveable(saver = PlayerSheet.Saver) { PlayerSheet() }
    BackHandler(enabled = navigator.canGoBack && !playerSheet.open) { navigator.back() }
    BackHandler(enabled = playerSheet.open) { playerSheet.open = false }
    // 주행 중에는 펼치지 않는다(펼친 채 출발하면 접는다).
    LaunchedEffect(driveMode) { if (driveMode == DriveMode.DRIVING) playerSheet.open = false }
    // 디버그 실행(--es screen Player)·예전 경로로 Player 화면이 열리면 시트로 바꾼다.
    LaunchedEffect(navigator.current) {
        if (navigator.current == Screen.Player) {
            navigator.back()
            if (driveMode == DriveMode.PARKED) playerSheet.open = true
        }
    }
    LaunchedEffect(launch.id) { launch.screen?.let(navigator::reset) }
    // 주행 상태가 바뀌거나 맞지 않는 화면이 열리면 그 상태의 홈으로 보낸다.
    LaunchedEffect(driveMode, navigator.current) { navigator.syncWithDriveMode(driveMode) }
    var demoPanelOpen by remember { mutableStateOf(false) }

    val situation = services?.situation
    var pendingIntro by remember { mutableStateOf<String?>(null) }
    var pendingChapter by remember { mutableStateOf<Int?>(null) }
    // 원고 보기로 열 챕터(완료한 챕터 줄을 눌렀을 때). null이면 지금 쓰는 챕터.
    var scriptChapter by rememberSaveable { mutableStateOf<Int?>(null) }

    // 첫 실행: 책이 하나도 없으면(첫 설치, 마지막 책을 지움) 정차 중에 첫 실행 화면으로 시작한다.
    // 첫 실행 중에 책이 생기면(시연용 서재 준비 포함) 홈으로 간다.
    val library = services?.library?.collectAsState()?.value
    val hasBook = library?.let { it.active != null }
    var firstRun by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(hasBook, driveMode, if (hasBook == false) navigator.current else null) {
        when {
            hasBook == false && driveMode == DriveMode.PARKED && navigator.current !in ONBOARDING -> {
                firstRun = true
                navigator.reset(Screen.Onboarding)
            }
            hasBook == true && firstRun -> {
                firstRun = false
                if (navigator.current in ONBOARDING) navigator.reset(Screen.Main)
            }
        }
    }
    val draft = rememberOnboardingDraft()

    // 인터뷰는 인터뷰 화면이 열려 있는 동안만 돈다. 원고 듣기도 같다. 둘 다 다른 소리를 멈춘다.
    val current = navigator.current
    LaunchedEffect(current, services) {
        services ?: return@LaunchedEffect
        if (current == Screen.Interview) {
            services.interview.start(intro = pendingIntro, chapter = pendingChapter)
            pendingIntro = null
            pendingChapter = null
        } else {
            services.interview.stop()
        }
        if (current == Screen.ListenDrive) services.listen.start() else services.listen.stop()
        services.setTalking(current == Screen.Interview || current == Screen.ListenDrive)
    }
    val book = services?.book?.collectAsState(initial = BookState())?.value ?: BookState()
    val latestBook by rememberUpdatedState(book)
    val latestDriveMode by rememberUpdatedState(driveMode)
    // “여기까지”·“오토야, 잠깐만”: 저장은 이미 되어 있다. 오토가 정리하는 화면 → 요약 화면으로 간다.
    // 요약은 인터뷰 위에 쌓지 않고 바꿔 끼운다(뒤로 가기로 인터뷰가 다시 시작되지 않게).
    val stopInterview = { navigator.showSummary() }
    LaunchedEffect(services) {
        services?.interview?.events?.collect { event ->
            when (event) {
                InterviewEvent.StopRequested -> stopInterview()
                // 인터뷰 중 “들려줘”: 원고 듣기로 넘어간다. 그 밖의 명령은 무시하고 계속 듣는다.
                is InterviewEvent.Command -> if (event.command == VoiceCommand.LISTEN) navigator.navigate(Screen.ListenDrive)
                is InterviewEvent.ChapterFinished -> when {
                    // 다음 챕터를 이어서 시작했으면 인터뷰 화면 그대로.
                    event.next != null -> Unit
                    // 12개 챕터를 모두 채웠으면 자서전 완성 화면.
                    (latestBook.done + event.chapter).size >= TOTAL_CHAPTERS -> navigator.navigate(Screen.Complete)
                    // 챕터를 마무리했으면(‘여기까지’·챕터 마무리) 요약 화면.
                    else -> stopInterview()
                }
            }
        }
    }
    // 인터뷰 밖에서 들린 말을 명령으로 처리한다.
    LaunchedEffect(services) {
        services?.speechInput?.utterances?.collect { text ->
            when (VoiceCommand.parseCommand(text)) {
                VoiceCommand.ACCEPT -> if (navigator.current == Screen.Nudge) navigator.reset(Screen.Interview)
                VoiceCommand.STOP -> when (navigator.current) {
                    Screen.Interview -> stopInterview()
                    Screen.ListenDrive -> navigator.navigate(if (navigator.backStack.contains(Screen.Main)) Screen.Main else Screen.Driving)
                    else -> Unit
                }
                VoiceCommand.FIX -> if (navigator.current == Screen.ListenDrive) services.listen.applyFix(text)
                VoiceCommand.LISTEN -> when (navigator.current) {
                    Screen.Main -> navigator.openFromMain(Screen.ListenDrive)
                    // 인터뷰 중(오토가 묻는 동안 포함)에도 원고 듣기로 넘어간다. 답변은 이미 저장되어 있다.
                    Screen.Driving, Screen.Interview, Screen.Summary -> navigator.navigate(Screen.ListenDrive)
                    else -> Unit
                }
                VoiceCommand.WHATS_UP -> if (navigator.current.drivingOnly || navigator.current == Screen.Main || navigator.current == Screen.Summary) {
                    val title = InterviewSession.titleOf(latestBook.current)
                    pendingIntro = "‘$title’ 부분을 다듬고 있었어요. 여기에 대해 조금 더 이야기해 주시겠어요?"
                    when {
                        navigator.current == Screen.Main -> navigator.openFromMain(Screen.Interview)
                        // 요약 화면에서는 요약을 인터뷰로 바꿔 끼운다.
                        navigator.current == Screen.Summary && navigator.backStack.contains(Screen.Main) -> navigator.openFromMain(Screen.Interview)
                        navigator.current == Screen.Summary -> navigator.reset(Screen.Interview)
                        else -> navigator.navigate(Screen.Interview)
                    }
                }
                else -> Unit
            }
        }
    }

    // 상황 트리거: 지금 화면을 알리고, 결정을 화면 전환으로 옮긴다.
    LaunchedEffect(navigator.current, situation) {
        situation?.onScreen(
            interviewActive = navigator.current == Screen.Interview || navigator.current == Screen.ListenDrive,
            nudgeVisible = navigator.current == Screen.Nudge,
        )
    }
    LaunchedEffect(situation) {
        situation?.actions?.collect { action ->
            when (action) {
                is TriggerAction.Nudge -> {
                    // 혼자 운전하던 사람이 놀라지 않게 멜로디를 먼저 울린다.
                    services?.playChime?.invoke()
                    navigator.navigate(Screen.Nudge)
                }
                TriggerAction.Defer -> navigator.navigate(Screen.Driving)
                is TriggerAction.WrapUp -> navigator.navigate(Screen.Pause)
                TriggerAction.CloseNudge -> if (navigator.current == Screen.Nudge) navigator.navigate(Screen.Driving)
                TriggerAction.SuggestRest -> Unit
            }
        }
    }
    val navi = situation?.navi?.collectAsState()?.value
    val nudgeReason = situation?.nudgeReason?.collectAsState()?.value
    val notice = situation?.notice?.collectAsState()?.value
    val liveProgress = services?.currentProgress?.collectAsState(initial = null)?.value
    val interviewUi = services?.interview?.ui?.collectAsState()?.value ?: InterviewUi()
    val radio = services?.radio?.collectAsState()?.value ?: RadioPlayback()
    val radioState = services?.radioPlayer?.state?.collectAsState()?.value ?: RadioState()
    val driveSituation = DriveSituation(
        minutesToArrival = navi?.minutesToArrival ?: DemoData.drive.minutesToArrival,
        nudgeReason = nudgeReason ?: NudgeReason.CONGESTION,
        notice = notice,
    )

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(AutoColors.Bg)
            .safeDrawingPadding(),
    ) {
        val scale = maxHeight.value / DesignFrame.HEIGHT
        val twoPane = maxWidth.value / scale >= TWO_PANE_MIN_WIDTH

        AutographyTheme(designScale = scale, driveMode = driveMode, demoOptions = launch.demoOptions) {
            CompositionLocalProvider(
                LocalDriveSituation provides driveSituation,
                LocalLiveProgress provides liveProgress,
                LocalInterviewUi provides interviewUi,
                LocalRadioPlayback provides radio,
                LocalRadioState provides radioState,
                LocalOnboardingDraft provides draft,
                LocalStartChapter provides { chapter -> pendingChapter = chapter },
                LocalPlayerSheet provides playerSheet,
                LocalScriptChapter provides ScriptChapter(scriptChapter) { scriptChapter = it },
                LocalBookState provides book,
                LocalAppServices provides services,
            ) {
                if (navigator.current == Screen.Pause) {
                    ScreenContent(Screen.Pause, navigator, Modifier.fillMaxSize())
                } else {
                    CompositionLocalProvider(LocalShowStatusChip provides !twoPane) {
                        AppPanes(
                            navigator = navigator,
                            driveMode = driveMode,
                            twoPane = twoPane,
                            onLogoLongPress = onDemoEvent?.let { { demoPanelOpen = !demoPanelOpen } },
                        )
                    }
                    if (twoPane) WindowStatusChip(navigator.current, driveMode, Modifier.align(Alignment.TopEnd))
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
}

@Composable
private fun AppPanes(
    navigator: AppNavigator,
    driveMode: DriveMode,
    twoPane: Boolean,
    onLogoLongPress: (() -> Unit)?,
) {
    val onboarding = navigator.current in ONBOARDING
    Row(Modifier.fillMaxSize()) {
        Spine(
            // 첫 실행 중에는 지금 정하고 있는 제목을 쓴다.
            bookTitle = if (onboarding) LocalOnboardingDraft.current.chosenTitle else LocalBookState.current.title,
            onSettings = { navigator.navigate(Screen.Settings) },
            onLogoLongPress = onLogoLongPress,
            settingsOpen = navigator.current == Screen.Settings,
            settingsEnabled = !onboarding,
        )
        if (onboarding) {
            // 첫 실행은 가로 전체 창에서도 한 칸으로 연다.
            ScreenContent(navigator.current, navigator, Modifier.weight(1f))
        } else if (twoPane && driveMode == DriveMode.DRIVING) {
            val talking = navigator.current.takeIf { it.driving } ?: Screen.Driving
            ScreenContent(talking, navigator, Modifier.width(FULL_LEFT_PANE_WIDTH.px), fullWindow = true)
            VLine(color = AutoColors.Ink)
            if (talking == Screen.Interview || talking == Screen.ListenDrive) {
                MyBookSidePane(Modifier.weight(1f))
            } else {
                RadioSidePane(Modifier.weight(1f))
            }
        } else if (twoPane) {
            val detail = navigator.current.takeIf { it.role == PaneRole.DETAIL && it != Screen.Player }
            // 왼쪽 목록 칸: 미니 플레이어의 ⌃를 누르면 플레이어가 이 칸 안에서 아래→위로 올라와 목록을 덮는다.
            Box(Modifier.width(FULL_LEFT_PANE_WIDTH.px).fillMaxHeight()) {
                ScreenContent(Screen.Main, navigator, Modifier.fillMaxSize(), fullWindow = true)
                PlayerSheetHost(Modifier.fillMaxSize())
            }
            VLine(color = AutoColors.Ink)
            if (detail != null) {
                // 창 오른쪽 위 상태 칩과 겹치지 않게 오른쪽 칸 위에 칩 높이만큼 비운다.
                ScreenContent(detail, navigator, Modifier.weight(1f).padding(top = DETAIL_CHIP_INSET.px), fullWindow = true)
            } else {
                ParkedWelcomePane(Modifier.weight(1f))
            }
        } else {
            // 세로 한쪽 창: 플레이어는 화면 전체(책등 오른쪽)를 아래→위로 덮는다.
            Box(Modifier.weight(1f).fillMaxHeight()) {
                ScreenContent(navigator.current.takeIf { it != Screen.Player } ?: Screen.Main, navigator, Modifier.fillMaxSize())
                if (driveMode == DriveMode.PARKED) PlayerSheetHost(Modifier.fillMaxSize())
            }
        }
    }
}

/** 플레이어 시트가 열려 있는지. 저장해 두어 창 크기가 바뀌어도 유지한다. */
@Stable
private class PlayerSheet(open: Boolean = false) {
    var open by mutableStateOf(open)

    companion object {
        val Saver = androidx.compose.runtime.saveable.Saver<PlayerSheet, Boolean>(save = { it.open }, restore = { PlayerSheet(it) })
    }
}

private val LocalPlayerSheet = staticCompositionLocalOf { PlayerSheet() }

/** 펼치는 시간(약 300ms). 사용자가 누를 때만 일어나는 전환이라 주행 중 자동 애니메이션 규칙에 걸리지 않는다. */
private const val PLAYER_SHEET_MS = 300

/**
 * 오토그래피 라디오 플레이어 시트: 미니 플레이어 자리(아래)에서 위로 올라와 그 칸을 덮고, 접으면 아래로 내려간다.
 * 위쪽 ⌄로 접는다.
 */
@Composable
private fun PlayerSheetHost(modifier: Modifier = Modifier) {
    val sheet = LocalPlayerSheet.current
    AnimatedVisibility(
        visible = sheet.open,
        modifier = modifier,
        enter = slideInVertically(tween(PLAYER_SHEET_MS, easing = FastOutSlowInEasing)) { it },
        exit = slideOutVertically(tween(PLAYER_SHEET_MS, easing = FastOutLinearInEasing)) { it },
    ) {
        PlayerScreen(
            onCollapse = { sheet.open = false },
            modifier = Modifier
                .fillMaxSize()
                .background(AutoColors.Bg)
                // 아래 목록이 눌리지 않게 시트가 터치를 받는다.
                .pointerInput(Unit) { detectTapGestures { } },
        )
    }
}

@Composable
private fun ScreenContent(
    screen: Screen,
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
    fullWindow: Boolean = false,
) {
    val services = LocalAppServices.current
    val interviewUi = LocalInterviewUi.current
    val bookId = LocalBookState.current.id
    val driveModeNow = LocalDriveMode.current
    val startChapter = LocalStartChapter.current
    val playerSheet = LocalPlayerSheet.current
    val scriptChapter = LocalScriptChapter.current
    // ‘여기까지’: 다 채운 뒤라면 챕터를 마무리하고, 아니면 멈춘다. 둘 다 정리 중 → 요약 화면으로.
    val stopHere: () -> Unit = {
        if (interviewUi.chapterFull) services?.interview?.finishChapter(startNext = false) else navigator.showSummary()
    }
    when (screen) {
        Screen.Main -> MainScreen(
            onContinueWriting = { navigator.openFromMain(Screen.Interview) },
            onStartChapter = { chapter ->
                val book = bookId
                if (book != null) services?.launch { services.repository.setCurrentChapter(book, chapter) }
                startChapter(chapter)
                navigator.openFromMain(Screen.Interview)
            },
            onListen = { playerSheet.open = true },
            onOpenScript = {
                scriptChapter.select(null)
                navigator.openFromMain(Screen.Script)
            },
            onStopHere = stopHere,
            onOpenChapterScript = { chapter ->
                scriptChapter.select(chapter)
                navigator.openFromMain(Screen.Script)
            },
            onOpenPlayer = { playerSheet.open = true },
            modifier = modifier,
            showHeader = !fullWindow,
            // 완료한 챕터의 원고를 열었으면 펼친(지금 쓰는) 챕터의 버튼은 고른 것으로 보이지 않는다.
            selectedAction = if (fullWindow && (navigator.current != Screen.Script || scriptChapter.chapter == null)) navigator.current.chapterAction() else null,
            radioPaused = LocalRadioPlayback.current.paused,
        )
        Screen.Driving -> DrivingScreen(
            onCallOto = { navigator.navigate(Screen.Interview) },
            onOpenPlayer = { playerSheet.open = true },
            modifier = modifier,
            showMiniPlayer = !fullWindow,
        )
        Screen.Interview -> InterviewScreen(
            parkedPane = fullWindow && LocalDriveMode.current == DriveMode.PARKED,
            onStopHere = stopHere,
            fullActions = ChapterFullActions(
                onFinish = { services?.interview?.finishChapter(startNext = false) },
                onNextChapter = { services?.interview?.finishChapter(startNext = true) },
                onReadScript = {
                    scriptChapter.select(null)
                    if (navigator.backStack.contains(Screen.Main)) navigator.openFromMain(Screen.Script) else navigator.navigate(Screen.Script)
                },
            ),
            modifier = modifier,
        )
        Screen.Script -> ScriptScreen(
            chapterNumber = scriptChapter.chapter,
            onHome = { navigator.navigate(Screen.Main) },
            modifier = modifier,
            showHome = !fullWindow,
        )
        Screen.ListenDrive -> ListenDriveScreen(
            onStopHere = { navigator.navigate(if (navigator.backStack.contains(Screen.Main)) Screen.Main else Screen.Driving) },
            // 시연: 마이크를 누르면 준비된 정정 문장을 말한 것으로 친다(Pleos SpeechToText 승인 전).
            onCallOto = { services?.let { it.speechInput.inject(it.demoFixSentence) } },
            modifier = modifier,
        )
        // 플레이어는 시트(PlayerSheetHost)로 그린다. 여기로 오는 Player는 곧바로 시트로 바뀐다.
        Screen.Player -> Unit
        Screen.Settings -> {
            val draft = LocalOnboardingDraft.current
            val settings = services?.library?.collectAsState()?.value?.settings
            SettingsScreen(
                onHome = { navigator.navigate(Screen.Main) },
                onNewBook = {
                    draft.reset(settings?.ownerName, settings?.callName)
                    navigator.navigate(Screen.Onboarding)
                },
                modifier = modifier,
                showHome = !fullWindow,
            )
        }
        Screen.Onboarding, Screen.Onboarding2, Screen.Onboarding3 -> {
            val step = ONBOARDING.indexOf(screen)
            val draft = LocalOnboardingDraft.current
            val scope = rememberCoroutineScope()
            // 첫 단계의 ‘이전’은 이미 책이 있을 때(새 자서전 시작)만 환경설정으로 돌아간다.
            val hasBook = LocalBookState.current.id != null
            OnboardingScreen(
                step = step + 1,
                draft = draft,
                previousEnabled = step > 0 || (hasBook && navigator.canGoBack),
                onPrevious = { navigator.back() },
                onNext = {
                    val nextStep = ONBOARDING.getOrNull(step + 1)
                    if (nextStep != null) {
                        navigator.navigate(nextStep)
                    } else {
                        // 시작하기: 이름·제목·첫 챕터를 저장하고 그 책을 지금 쓰는 책으로 둔다.
                        scope.launch {
                            services?.createBook?.invoke(draft.chosenTitle, draft.name.trim(), draft.callName, draft.firstChapter)
                            navigator.reset(Screen.Main)
                        }
                    }
                },
                modifier = modifier,
            )
        }
        Screen.Complete -> CompleteScreen(
            onHome = { navigator.navigate(Screen.Main) },
            modifier = modifier,
        )
        Screen.Summary -> SummaryScreen(
            onHome = { navigator.navigate(if (driveModeNow == DriveMode.DRIVING) Screen.Driving else Screen.Main) },
            onSimilar = {
                if (driveModeNow == DriveMode.DRIVING) {
                    // 주행 중에는 라디오로 비슷한 이야기를 바로 틀고 주행 홈으로.
                    services?.radioPlayer?.playSimilar()
                    navigator.navigate(Screen.Driving)
                } else {
                    playerSheet.open = true
                }
            },
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

/** 펼친 챕터의 어느 버튼으로 연 화면인지(가로 전체 창에서 선택 표시). */
private fun Screen.chapterAction(): ChapterAction? = when (this) {
    Screen.Interview -> ChapterAction.CONTINUE
    Screen.Script -> ChapterAction.SCRIPT
    else -> null
}

/** 가로 전체 창의 상태 칩. 창 오른쪽 위 한 곳에만 둔다. */
@Composable
private fun WindowStatusChip(current: Screen, driveMode: DriveMode, modifier: Modifier = Modifier) {
    val text = when {
        driveMode == DriveMode.PARKED -> "정차 중 · P"
        current == Screen.Interview -> "주행 중 · 인터뷰"
        current == Screen.ListenDrive -> "주행 중 · 내 원고 고치기"
        else -> "주행 중 · 말로 불러 주세요"
    }
    StatusChip(
        text = text,
        filled = driveMode == DriveMode.DRIVING,
        modifier = modifier.padding(top = 36.px, end = 40.px),
    )
}

/** 홈에서 다른 챕터를 골라 인터뷰를 열 때, 그 챕터로 시작하게 알린다. */
private val LocalStartChapter = staticCompositionLocalOf<(Int) -> Unit> { {} }

/** 원고 보기로 열 챕터와 그 값을 바꾸는 함수. */
private class ScriptChapter(val chapter: Int?, val select: (Int?) -> Unit)

private val LocalScriptChapter = staticCompositionLocalOf { ScriptChapter(null) {} }

/** 첫 실행 세 단계(순서대로). */
private val ONBOARDING = listOf(Screen.Onboarding, Screen.Onboarding2, Screen.Onboarding3)
