package com.pleosautography.app.domain.interview

import android.util.Log
import com.pleosautography.app.data.audio.RadioAudio
import com.pleosautography.app.data.db.QuestionEntity
import com.pleosautography.app.data.demo.DemoData
import com.pleosautography.app.data.repo.AutographyRepository
import com.pleosautography.app.data.speech.Heard
import com.pleosautography.app.data.speech.SpeechInput
import com.pleosautography.app.data.speech.SpeechOutput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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

    /** 오토가 말하는 중(질문·후속 질문·안내). */
    ASKING,

    /** 운전자의 답을 듣는 중. */
    LISTENING,
}

/**
 * @param prompt 화면에 쓸 오토의 말(질문이나 후속 질문). null이면 화면이 다음 질문을 쓴다.
 * @param chapterFull 챕터의 질문을 모두 채운 상태(시안 ChapterFull·ChapterDone). 계속 듣는다.
 */
data class InterviewUi(
    val phase: InterviewPhase = InterviewPhase.IDLE,
    val chapter: Int = 0,
    val prompt: String? = null,
    val question: QuestionEntity? = null,
    val level: Float = 0f,
    val chapterFull: Boolean = false,
)

sealed interface InterviewEvent {
    /** “오토야, 잠깐만”: 멈춰 달라고 했다(저장은 이미 끝났다). */
    data object StopRequested : InterviewEvent

    /** 챕터를 마무리했다. [next]가 있으면 그 챕터 인터뷰를 이어서 시작했다. */
    data class ChapterFinished(val chapter: Int, val next: Int?) : InterviewEvent
}

/**
 * 인터뷰 루프(CLAUDE.md 인터뷰 엔진).
 * 질문 → 듣기 → 말을 멈추면 받아 적어 즉시 저장 → 방금 이야기를 받아 되묻는 후속 질문 → … 을 되풀이한다.
 * 질문을 모두 채우면 “다 채웠어요” 안내를 하고 계속 듣는다. 더 말하면 덧붙인 이야기로 저장하고,
 * “여기까지”라고 하면 챕터를 마무리한다.
 */
class InterviewSession(
    private val scope: CoroutineScope,
    private val repository: AutographyRepository,
    private val input: SpeechInput,
    private val output: SpeechOutput,
    private val audio: RadioAudio,
    private val followUps: FollowUpGenerator,
) {
    private val state = MutableStateFlow(InterviewUi())
    val ui: StateFlow<InterviewUi> = state.asStateFlow()

    private val eventFlow = MutableSharedFlow<InterviewEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<InterviewEvent> = eventFlow.asSharedFlow()

    private val chapterState = MutableStateFlow(FIRST_WRITING_CHAPTER)

    /** 지금 쓰는 챕터. */
    val currentChapter: StateFlow<Int> = chapterState.asStateFlow()

    private var job: Job? = null

    /** 인터뷰를 시작한다. 이미 진행 중이면 그대로 둔다. */
    fun start(intro: String? = null) {
        if (job?.isActive == true) return
        audio.setTalking(true)
        val chapter = chapterState.value
        job = scope.launch { run(chapter, intro) }
    }

    /** 인터뷰를 멈춘다. 답변은 받아 적을 때마다 저장했으므로 따로 저장할 것이 없다. */
    fun stop() {
        job?.cancel()
        job = null
        state.value = InterviewUi()
        audio.setTalking(false)
    }

    /**
     * 챕터를 마무리(완성)한다.
     * @param startNext 다음 챕터 인터뷰를 바로 이어서 시작한다(‘다음 챕터’ 버튼).
     */
    fun finishChapter(startNext: Boolean) {
        val chapter = chapterState.value
        job?.cancel()
        job = null
        scope.launch {
            repository.markChapterDone(chapter)
            // 다음에 쓸 챕터로 넘어간다. ‘다음 챕터’면 바로 인터뷰를 이어 간다.
            val following = nextChapterAfter(chapter)
            if (following != null) chapterState.value = following
            val next = following.takeIf { startNext }
            if (next != null) {
                val title = titleOf(next)
                start(intro = "좋아요. 이번에는 ‘$title’ 이야기를 해 볼까요?")
            } else {
                // 정차 중이면 같은 화면에 남아 원고를 볼 수 있게, 다 채운 상태로 둔다.
                state.update { it.copy(phase = InterviewPhase.IDLE, chapter = chapter, chapterFull = true) }
            }
            eventFlow.tryEmit(InterviewEvent.ChapterFinished(chapter, next))
        }
    }

    private suspend fun run(chapter: Int, intro: String?) {
        var prompt: String? = null
        intro?.let { say(chapter, it, null) }
        while (true) {
            val progress = repository.observeProgress(chapter).first()
            val question = progress.nextQuestion
            if (question == null) {
                if (!listenWhenFull(chapter)) return
                continue
            }
            val text = prompt ?: question.text
            say(chapter, text, question, prompt = text)

            state.value = InterviewUi(InterviewPhase.LISTENING, chapter, text, question)
            val heard = input.listen(question.id) { level -> state.update { it.copy(level = level) } }
            if (heard !is Heard.Text) {
                prompt = text // 아무 말이 없으면 같은 말을 다시 기다린다.
                continue
            }
            if (VoiceCommand.parse(heard.text) == VoiceCommand.STOP) {
                eventFlow.tryEmit(InterviewEvent.StopRequested)
                return
            }
            repository.saveAnswer(question, heard.text, heard.durationSec)
            Log.i(TAG, "저장: ${question.id} ${heard.text}")

            // 방금 이야기를 받아 되묻는 후속 질문(다음 질문으로 이어진다).
            val next = repository.observeProgress(chapter).first().nextQuestion
            prompt = next?.let { followUps.followUp(question, heard.text, it) }
        }
    }

    /**
     * 질문을 모두 채운 뒤: 안내하고 계속 듣는다. 더 말하면 덧붙인 이야기로 저장한다.
     * @return 계속 들을지. “여기까지”를 들으면 챕터를 마무리하고 false.
     */
    private suspend fun listenWhenFull(chapter: Int): Boolean {
        val announce = fullPrompt(chapter)
        if (!state.value.chapterFull) say(chapter, announce, null, prompt = announce, full = true)
        state.value = InterviewUi(InterviewPhase.LISTENING, chapter, announce, chapterFull = true)
        val heard = input.listen(AutographyRepository.extraQuestionId(chapter)) { level ->
            state.update { it.copy(level = level) }
        }
        if (heard !is Heard.Text) return true
        if (VoiceCommand.parse(heard.text) == VoiceCommand.STOP) {
            finishChapter(startNext = false)
            return false
        }
        repository.saveExtra(chapter, heard.text, heard.durationSec)
        Log.i(TAG, "덧붙임 저장: $chapter ${heard.text}")
        return true
    }

    private suspend fun say(chapter: Int, text: String, question: QuestionEntity?, prompt: String? = null, full: Boolean = false) {
        state.value = InterviewUi(InterviewPhase.ASKING, chapter, prompt, question, chapterFull = full)
        audio.ducking { output.speak(text.replace('\n', ' ')) }
    }

    private suspend fun nextChapterAfter(chapter: Int): Int? {
        val candidate = chapter + 1
        return candidate.takeIf { repository.observeProgress(it).first().total > 0 }
    }

    companion object {
        private const val TAG = "InterviewSession"

        /** 시연에서 처음 쓰는 챕터(2장 ‘첫 차’). Book·Chapter 테이블이 생기면 저장된 값을 읽는다. */
        const val FIRST_WRITING_CHAPTER = 2

        fun titleOf(chapter: Int): String = DemoData.chapters.firstOrNull { it.number == chapter }?.title ?: "${chapter}장"

        /** 받침이 있으면 “을”, 없으면 “를”. */
        fun objectParticle(word: String): String {
            val last = word.lastOrNull { it in '가'..'힣' } ?: return "를"
            return if ((last - '가') % 28 != 0) "을" else "를"
        }

        fun fullPrompt(chapter: Int) =
            "‘${titleOf(chapter)}’${objectParticle(titleOf(chapter))} 다 채웠어요!\n더 하실 이야기는 이어서,\n마무리는 “여기까지”라고\n말씀해 주세요."
    }
}
