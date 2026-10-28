package com.pleosautography.app

import android.Manifest
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
import com.pleosautography.app.ui.AppServices
import com.pleosautography.app.ui.AutographyApp
import com.pleosautography.app.ui.LaunchRequest
import com.pleosautography.app.ui.navigation.Screen
import com.pleosautography.app.ui.screens.BookState
import com.pleosautography.app.ui.theme.DemoOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {

    private val graph by lazy { AppGraph.get(this) }
    private val debuggable by lazy { (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 }
    private var launch by mutableStateOf(LaunchRequest())

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result[Car.PERMISSION_SPEED] == true) graph.carVehicle.onSpeedPermissionGranted()
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) launch = readLaunchRequest(intent)
        // 속도(주행 판정)와 마이크(인터뷰). 정차 중 첫 실행에 한 번 묻는다.
        val missing = listOf(Car.PERMISSION_SPEED, Manifest.permission.RECORD_AUDIO)
            .filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) requestPermissions.launch(missing.toTypedArray())
        val repository = graph.repository
        val services = AppServices(
            situation = graph.situation,
            interview = graph.interview,
            listen = graph.listen,
            speechInput = graph.speechInput,
            radio = graph.radioAudio.radio,
            radioPlayer = graph.radioPlayer,
            repository = repository,
            library = repository.library,
            currentProgress = repository.currentProgress,
            book = repository.library.flatMapLatest { library ->
                val active = library?.active ?: return@flatMapLatest flowOf(BookState(callName = library?.settings?.callName.orEmpty()))
                repository.observeBook(active.id).map { content ->
                    BookState(
                        id = active.id,
                        title = active.title,
                        callName = library.settings?.callName ?: active.ownerCallName,
                        current = active.currentChapter,
                        done = content.done,
                        stories = content.stories,
                        stats = content.stats,
                        publishRequested = active.publishRequestedAt != null,
                    )
                }
            },
            answersFor = { chapter ->
                repository.activeBookId.distinctUntilChanged().flatMapLatest { id ->
                    id?.let { repository.observeAnswers(it, chapter) } ?: flowOf(emptyList())
                }
            },
            playChime = {
                if (repository.library.value?.settings?.chimeOn != false) graph.radioAudio.ducking { graph.chime.play() }
            },
            setTalking = graph.radioAudio::setTalking,
            readAloud = { text -> graph.radioAudio.ducking { graph.speechOutput.speak(text) } },
            createBook = graph::createBook,
            launch = graph::launch,
            demoFixSentence = graph.demoScript.fix,
            setDemoMode = graph::setDemoMode,
            loadSampleBook = graph::loadSampleBook,
        )
        setContent {
            val driveMode by graph.driveMode.collectAsState()
            val demoMode by graph.demoMode.collectAsState()
            AutographyApp(
                driveMode = driveMode,
                launch = launch,
                // 데모 패널: 디버그 빌드는 늘, release는 환경설정의 시연 모드를 켰을 때만. adb 리시버는 디버그 전용이다.
                onDemoEvent = if (debuggable || demoMode) graph.demoEvents::send else null,
                demoEvents = if (debuggable) DemoEvent.entries else RELEASE_DEMO_EVENTS,
                demoModeNote = demoMode,
                services = services,
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
        // --es setup demo: 시연용 서재를 만든다(책이 없을 때만, demo.sh --reset 뒤).
        if (intent.getStringExtra("setup") == "demo") graph.demoEvents.send(DemoEvent.SETUP)
        val options = DemoOptions(timersRunning = intent.getBooleanExtra("timers", true))
        return LaunchRequest(id = System.nanoTime(), screen = screen, demoOptions = options)
    }
}

/** release 시연 모드의 데모 패널에 보일 이벤트. */
private val RELEASE_DEMO_EVENTS = listOf(
    DemoEvent.DRIVE,
    DemoEvent.PARK,
    DemoEvent.REAL_VEHICLE,
    DemoEvent.SAY_WHATS_UP,
    DemoEvent.ANSWER_DONE,
    DemoEvent.SAY_STOP,
    DemoEvent.FINISH,
    DemoEvent.CONGESTION_15,
    DemoEvent.ARRIVAL_10,
)
