package com.pleosautography.app

import android.content.Context
import android.content.pm.ApplicationInfo
import com.pleosautography.app.data.audio.ChimePlayer
import com.pleosautography.app.data.audio.RadioAudio
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

    // 저장소(기기 안 Room DB)와 질문 은행
    private val database = AppDatabase.create(context)
    val repository = AutographyRepository(database, context.assets)

    // 음성: Pleos SpeechToText·TextToSpeech 승인 전까지 시연용 Fake(실제 마이크 음량 + 준비된 문장)
    private val demoScript = DemoScript.load(context)
    /** 말이 끝난 것으로 볼 침묵: 시연(디버그 빌드) 1초, 기본 1.5초. */
    val voiceSettings = VoiceSettings(if (debuggable) VoiceSettings.DEMO_SILENCE_END_MS else VoiceSettings.DEFAULT_SILENCE_END_MS)
    val speechInput: SpeechInput = FakeSpeechInput(context, demoScript, voiceSettings)
    val speechOutput: SpeechOutput = CaptionSpeechOutput()
    // 후속 질문: 시연용 준비 문장(2단계에서 AI 생성으로 바꿔 끼운다)
    val interview = InterviewSession(scope, repository, speechInput, speechOutput, radioAudio, ScriptedFollowUps(demoScript))

    val demoEvents = DemoEvents(vehicle, speechInput, interview)

    /** 경로 안내. Pleos NaviHelper 구현체는 마일스톤 8에서 붙이고, 그전에는 시연용 Fake를 쓴다. */
    val navi = FakeNaviState()
    val situation = SituationController(scope, vehicle, navi, demoEvents.events)

    /** 주행/정차. 시작값도 지금 차량 값으로 바로 계산해 화면이 한 번 잘못 바뀌지 않게 한다. */
    val driveMode: StateFlow<DriveMode> = vehicle.snapshot
        .map(DriveModeRule::resolve)
        .stateIn(scope, SharingStarted.Eagerly, DriveModeRule.resolve(vehicle.snapshot.value))

    init {
        carVehicle.start()
        scope.launch {
            repository.syncQuestions()
            // 시연용 예시 답변(디버그 빌드만): 첫 실행부터 ‘첫 차’ 3편이 있는 상태로 시작한다.
            if (debuggable) repository.seedDemoAnswersIfEmpty(demoScript.seed)
        }
    }

    companion object {
        @Volatile private var instance: AppGraph? = null

        fun get(context: Context): AppGraph =
            instance ?: synchronized(this) {
                instance ?: AppGraph(context.applicationContext).also { instance = it }
            }
    }
}
