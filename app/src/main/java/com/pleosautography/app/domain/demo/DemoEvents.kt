package com.pleosautography.app.domain.demo

import android.util.Log
import com.pleosautography.app.data.speech.SpeechInput
import com.pleosautography.app.data.vehicle.DemoVehicleState
import com.pleosautography.app.domain.interview.InterviewSession
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 시연 상황을 버튼 하나(또는 adb 한 줄)로 재현하는 데모 이벤트.
 * adb 이름은 [id]다: `adb shell am broadcast -a com.pleosautography.app.DEMO_EVENT -p com.pleosautography.app --es event drive`
 */
enum class DemoEvent(val id: String, val label: String) {
    DRIVE("drive", "주행 (D · 40km/h)"),
    PARK("park", "정차 (P)"),
    REAL_VEHICLE("real", "실제 차량 값"),

    // 상황 트리거(SituationController가 처리)
    CONGESTION_15("congestion15", "정체 15분"),
    STRAIGHT_15("straight15", "긴 직진 15분"),
    COMPLEX_START("complex_start", "복잡 구간 시작"),
    COMPLEX_END("complex_end", "복잡 구간 끝"),
    ARRIVAL_10("arrival10", "도착 10분 전"),

    // 음성(시연용 Fake 음성 입력에 넣는다)
    SAY_YES("yes", "“응”"),
    SAY_STOP("stop", "“오토야, 잠깐만”"),
    SAY_WHATS_UP("whatsup", "“오토야, 뭐해?”"),
    SAY_LISTEN("listen", "“들려줘”"),
    SAY_FIX("fix", "“방금 거기 고치자”"),
    ANSWER_DONE("answer", "말 끝내기"),

    // 챕터를 다 채운 뒤
    MORE("more", "더 이야기하기"),
    FINISH("finish", "“여기까지” 챕터 마무리"),

    // 시연 준비
    SETUP("setup", "시연용 자서전 준비"),
    ;

    companion object {
        fun fromId(id: String?): DemoEvent? = entries.firstOrNull { it.id == id }
    }
}

/** 데모 이벤트를 받아 Fake 상태에 반영한다. 차량 상태 밖의 이벤트는 [events]로 흘려보낸다. */
class DemoEvents(
    private val vehicle: DemoVehicleState,
    private val speech: SpeechInput,
    private val interview: InterviewSession,
    /** 원고 듣기에서 말할 준비된 정정 문장. */
    private val fixSentence: String,
    /** 시연용 서재를 만든다(책이 없을 때만). */
    private val setupDemo: () -> Unit,
) {
    private val flow = MutableSharedFlow<DemoEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<DemoEvent> = flow.asSharedFlow()

    fun send(event: DemoEvent) {
        Log.i(TAG, "데모 이벤트: ${event.id}")
        when (event) {
            DemoEvent.DRIVE -> vehicle.override(DemoVehicleState.Driving)
            DemoEvent.PARK -> vehicle.override(DemoVehicleState.Parked)
            DemoEvent.REAL_VEHICLE -> vehicle.override(null)
            DemoEvent.SAY_YES -> speech.inject("응")
            DemoEvent.SAY_STOP -> speech.inject("오토야, 잠깐만")
            DemoEvent.SAY_WHATS_UP -> speech.inject("오토야, 뭐해?")
            DemoEvent.SAY_LISTEN -> speech.inject("오토야, 이 이야기 들려줘")
            DemoEvent.SAY_FIX -> speech.inject(fixSentence)
            DemoEvent.ANSWER_DONE, DemoEvent.MORE -> speech.finishNow()
            DemoEvent.FINISH -> interview.finishChapter(startNext = false)
            DemoEvent.SETUP -> setupDemo()
            else -> Unit
        }
        flow.tryEmit(event)
    }

    private companion object {
        const val TAG = "DemoEvents"
    }
}
