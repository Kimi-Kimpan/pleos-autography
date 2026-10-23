package com.pleosautography.app.domain.interview

import android.util.Log
import com.pleosautography.app.data.audio.RadioAudio
import com.pleosautography.app.data.db.QuestionEntity
import com.pleosautography.app.data.repo.AutographyRepository
import com.pleosautography.app.data.speech.Heard
import com.pleosautography.app.data.speech.SpeechInput
import com.pleosautography.app.data.speech.SpeechOutput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 인터뷰 화면이 보여 줄 단계. */
enum class InterviewPhase {
    IDLE,

    /** 오토가 말하는 중(질문·안내). */
    ASKING,

    /** 운전자의 답을 듣는 중. */
    LISTENING,

    /** 받아 적어 저장했다. */
    SAVED,

    /** 챕터의 질문을 모두 마쳤다. */
    DONE,
}

data class InterviewUi(
    val phase: InterviewPhase = InterviewPhase.IDLE,
    val question: QuestionEntity? = null,
    val level: Float = 0f,
    val lastTranscript: String? = null,
)

/**
 * 인터뷰 루프(CLAUDE.md 인터뷰 엔진): 질문 TTS → 답변 STT → Answer 저장 → 진행률 갱신 → 다음 질문.
 * 받아 적는 즉시 저장하므로 언제 멈춰도 지금까지의 답변은 남는다.
 * “오토야, 잠깐만”을 들으면 [stopRequests]로 화면에 알린다(화면이 저장 후 주행 홈으로 간다).
 */
class InterviewSession(
    private val scope: CoroutineScope,
    private val repository: AutographyRepository,
    private val input: SpeechInput,
    private val output: SpeechOutput,
    private val audio: RadioAudio,
) {
    private val state = MutableStateFlow(InterviewUi())
    val ui: StateFlow<InterviewUi> = state.asStateFlow()

    private val stops = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val stopRequests: SharedFlow<Unit> = stops.asSharedFlow()

    private var job: Job? = null

    /** 인터뷰를 시작한다. 이미 진행 중이면 그대로 둔다. */
    fun start(chapter: Int, intro: String? = null) {
        if (job?.isActive == true) return
        audio.setTalking(true)
        job = scope.launch { run(chapter, intro) }
    }

    /** 인터뷰를 멈춘다. 답변은 받아 적을 때마다 저장했으므로 따로 저장할 것이 없다. */
    fun stop() {
        job?.cancel()
        job = null
        state.value = InterviewUi()
        audio.setTalking(false)
    }

    private suspend fun run(chapter: Int, intro: String?) {
        intro?.let { say(it, null) }
        while (true) {
            val progress = repository.observeProgress(chapter).first()
            val question = progress.nextQuestion
            if (question == null) {
                state.value = InterviewUi(InterviewPhase.DONE)
                say("이 챕터의 질문을 모두 마쳤어요. 정말 고마워요.", null)
                return
            }
            say(question.text.replace('\n', ' '), question)

            state.value = InterviewUi(InterviewPhase.LISTENING, question)
            val heard = input.listen(question.id) { level -> state.update { it.copy(level = level) } }
            if (heard !is Heard.Text) continue // 아무 말이 없으면 같은 질문을 다시 듣는다.

            if (VoiceCommand.parse(heard.text) == VoiceCommand.STOP) {
                stops.tryEmit(Unit)
                return
            }
            repository.saveAnswer(question, heard.text, heard.durationSec)
            Log.i(TAG, "저장: ${question.id} ${heard.text}")
            state.value = InterviewUi(InterviewPhase.SAVED, question, lastTranscript = heard.text)
            delay(SAVED_SHOW_MS)
        }
    }

    private suspend fun say(text: String, question: QuestionEntity?) {
        state.value = InterviewUi(InterviewPhase.ASKING, question)
        audio.ducking { output.speak(text) }
    }

    private companion object {
        const val TAG = "InterviewSession"
        const val SAVED_SHOW_MS = 2_500L
    }
}
