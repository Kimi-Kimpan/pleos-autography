package com.pleosautography.app

import android.content.Context
import android.content.pm.ApplicationInfo
import com.pleosautography.app.data.audio.ChimePlayer
import com.pleosautography.app.data.audio.RadioAudio
import com.pleosautography.app.data.audio.RadioPlayer
import com.pleosautography.app.data.db.AppDatabase
import com.pleosautography.app.data.navi.FakeNaviState
import com.pleosautography.app.data.repo.AutographyRepository
import com.pleosautography.app.data.speech.CaptionSpeechOutput
import com.pleosautography.app.data.speech.DemoScript
import com.pleosautography.app.data.speech.FakeSpeechInput
import com.pleosautography.app.data.speech.SpeechInput
import com.pleosautography.app.data.speech.SpeechOutput
import com.pleosautography.app.data.speech.VoiceSettings
import com.pleosautography.app.data.vehicle.CarApiVehicleState
import com.pleosautography.app.data.vehicle.DemoVehicleState
import com.pleosautography.app.domain.DriveMode
import com.pleosautography.app.domain.DriveModeRule
import com.pleosautography.app.domain.demo.DemoEvents
import com.pleosautography.app.domain.interview.InterviewSession
import com.pleosautography.app.domain.interview.ScriptedFollowUps
import com.pleosautography.app.domain.interview.VoiceCommand
import com.pleosautography.app.domain.listen.ListenSession
import com.pleosautography.app.domain.trigger.SituationController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 앱 전체에서 하나만 쓰는 객체들(간단한 수동 의존성 주입).
 * 차량 연결은 앱 프로세스 동안 유지한다.
 */
class AppGraph private constructor(context: Context) {
    private val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val carVehicle = CarApiVehicleState(context)
    val vehicle = DemoVehicleState(carVehicle, scope)
    val chime = ChimePlayer()
    val radioAudio = RadioAudio(context)
    val radioPlayer = RadioPlayer(scope, radioAudio)

    // 저장소(기기 안 Room DB)와 질문 은행
    private val database = AppDatabase.create(context)
    val repository = AutographyRepository(database, context.assets, scope)

    // 음성: Pleos SpeechToText·TextToSpeech 승인 전까지 시연용 Fake(실제 마이크 음량 + 준비된 문장)
    val demoScript = DemoScript.load(context)
    /** 말이 끝난 것으로 볼 침묵: 시연(디버그 빌드) 1초, 기본 1.5초. */
    val voiceSettings = VoiceSettings(if (debuggable) VoiceSettings.DEMO_SILENCE_END_MS else VoiceSettings.DEFAULT_SILENCE_END_MS)
    val speechInput: SpeechInput = FakeSpeechInput(context, demoScript, voiceSettings)
    val speechOutput: SpeechOutput = CaptionSpeechOutput()
    // 후속 질문: 시연용 준비 문장(2단계에서 AI 생성으로 바꿔 끼운다)
    val interview = InterviewSession(scope, repository, speechInput, speechOutput, radioAudio, ScriptedFollowUps(demoScript))
    val listen = ListenSession(scope, repository, speechOutput)

    val demoEvents = DemoEvents(vehicle, speechInput, interview, demoScript.fix, setupDemo = { scope.launch { setupDemoLibrary() } })

    /** 경로 안내. Pleos NaviHelper 구현체는 마일스톤 8에서 붙이고, 그전에는 시연용 Fake를 쓴다. */
    val navi = FakeNaviState()
    val situation = SituationController(
        scope, vehicle, navi, demoEvents.events,
        proactiveTalk = { repository.library.value?.settings?.proactiveTalk ?: true },
    )

    /** 주행/정차. 시작값도 지금 차량 값으로 바로 계산해 화면이 한 번 잘못 바뀌지 않게 한다. */
    val driveMode: StateFlow<DriveMode> = vehicle.snapshot
        .map(DriveModeRule::resolve)
        .stateIn(scope, SharingStarted.Eagerly, DriveModeRule.resolve(vehicle.snapshot.value))

    init {
        carVehicle.start()
        scope.launch {
            repository.syncQuestions()
            // 예전 빌드가 음성 명령(“들려줘”, “여기까지” 등)을 답변으로 저장한 것을 지운다.
            val removed = repository.deleteAnswersWhere { VoiceCommand.parseCommand(it.transcript) != null }
            if (removed > 0) android.util.Log.i("AppGraph", "명령 문장 답변 정리: ${removed}개")
        }
    }

    /** 화면이 닫혀도 끝까지 해야 하는 저장 작업(책 지우기·이름 바꾸기 등). */
    fun launch(block: suspend () -> Unit) {
        scope.launch { block() }
    }

    /** 시연용 서재(디버그 빌드만): 예시 답변이 든 ‘김○○의 자서전’(1장 완성, ‘첫 차’ 5편)과 다른 책 한 권. */
    suspend fun setupDemoLibrary() {
        if (debuggable) repository.setupDemoLibrary(demoScript.seed)
    }

    /**
     * 첫 실행·새 자서전을 마치면 책을 만든다. 디버그 빌드의 첫 책은 ‘첫 차’로 시작하면 예시 답변을 넣어
     * 시연을 ‘첫 차’ 71%부터 이어 갈 수 있게 한다.
     */
    suspend fun createBook(title: String, ownerName: String, callName: String, firstChapter: Int) {
        val first = repository.library.value?.books.isNullOrEmpty()
        val seed = demoScript.seed.takeIf { debuggable && first && firstChapter == InterviewSession.FIRST_WRITING_CHAPTER }
        repository.createBook(title, ownerName, callName, firstChapter, seed)
    }

    companion object {
        @Volatile private var instance: AppGraph? = null

        fun get(context: Context): AppGraph =
            instance ?: synchronized(this) {
                instance ?: AppGraph(context.applicationContext).also { instance = it }
            }
    }
}
