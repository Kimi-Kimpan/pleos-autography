package com.pleosautography.app.domain.interview

import android.util.Log
import com.pleosautography.app.data.audio.RadioAudio
import com.pleosautography.app.data.db.QuestionEntity
import com.pleosautography.app.data.demo.DemoData
import com.pleosautography.app.data.repo.AutographyRepository
import com.pleosautography.app.data.speech.Heard
import com.pleosautography.app.data.speech.SpeechInput
import com.pleosautography.app.data.speech.SpeechOutput
import com.pleosautography.app.domain.listen.ListenSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

/**
 * 인터뷰 한 번(시작 ~ ‘여기까지’)의 요약. ‘여기까지’ 뒤 요약 화면이 보인다.
 * @param sentences 이번에 새로 적힌 문장 수(덧붙인 이야기 포함).
 * @param answeredBefore 시작할 때 답한 질문 수. [answeredAfter]는 끝날 때.
 * @param chapterDone 챕터를 마무리(완성)했는지.
 */
data class InterviewSummary(
    val chapter: Int,
    val sentences: Int,
    val durationSec: Int,
    val answeredBefore: Int,
    val answeredAfter: Int,
    val total: Int,
    val chapterDone: Boolean,
) {
    val progressBefore: Float get() = if (total == 0) 0f else answeredBefore.toFloat() / total
    val progressAfter: Float get() = if (total == 0) 0f else answeredAfter.toFloat() / total

    /** 완성까지 남은 질문 수. */
    val remaining: Int get() = (total - answeredAfter).coerceAtLeast(0)
}

sealed interface InterviewEvent {
    /** “오토야, 잠깐만”: 멈춰 달라고 했다(저장은 이미 끝났다). */
    data object StopRequested : InterviewEvent

    /** 인터뷰 중 들린 다른 명령(“들려줘” 등). 답변으로 저장하지 않고 화면에 넘긴다. */
    data class Command(val command: VoiceCommand, val text: String) : InterviewEvent

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

    /** 지금 쓰는 챕터(지금 쓰는 책에 저장된 값). */
    val currentChapter: StateFlow<Int> = repository.library
        .map { it?.active?.currentChapter ?: FIRST_WRITING_CHAPTER }
        .stateIn(scope, SharingStarted.Eagerly, FIRST_WRITING_CHAPTER)

    private var job: Job? = null

    /** 지금 인터뷰 한 번의 기록(요약 화면용). */
    private class SessionStats(val chapter: Int, val startMs: Long, val answeredBefore: Int, val total: Int) {
        var sentences = 0
        var newAnswers = 0

        fun toSummary(chapterDone: Boolean) = InterviewSummary(
            chapter = chapter,
            sentences = sentences,
            durationSec = ((System.currentTimeMillis() - startMs) / 1000).toInt(),
            answeredBefore = answeredBefore,
            answeredAfter = if (chapterDone) total else (answeredBefore + newAnswers).coerceAtMost(total),
            total = total,
            chapterDone = chapterDone,
        )
    }

    @Volatile private var stats: SessionStats? = null

    private val summaryState = MutableStateFlow<InterviewSummary?>(null)

    /** 마지막으로 끝낸 인터뷰의 요약(‘여기까지’ 뒤 요약 화면). */
    val summary: StateFlow<InterviewSummary?> = summaryState.asStateFlow()

    /**
     * 인터뷰를 시작한다. 이미 진행 중이거나 쓰는 책이 없으면 그대로 둔다.
     * @param chapter 이 챕터로 시작한다(홈에서 다른 챕터를 고른 직후, 저장이 화면보다 늦을 수 있어서 직접 준다).
     */
    fun start(intro: String? = null, chapter: Int? = null) {
        if (job?.isActive == true) return
        val book = repository.activeBook() ?: return
        launchRun(book.id, chapter ?: book.currentChapter, intro)
    }

    private fun launchRun(book: Long, chapter: Int, intro: String?) {
        audio.setTalking(true)
        job = scope.launch { run(book, chapter, intro) }
    }

    /** 인터뷰를 멈춘다. 답변은 받아 적을 때마다 저장했으므로 따로 저장할 것이 없다. */
    fun stop() {
        job?.cancel()
        job = null
        stats?.let { summaryState.value = it.toSummary(chapterDone = false) }
        stats = null
        state.value = InterviewUi()
        audio.setTalking(false)
    }

    /**
     * 챕터를 마무리(완성)한다.
     * @param startNext 다음 챕터 인터뷰를 바로 이어서 시작한다(‘다음 챕터’ 버튼).
     */
    fun finishChapter(startNext: Boolean) {
        val book = repository.activeBook() ?: return
        val chapter = state.value.chapter.takeIf { it > 0 } ?: book.currentChapter
        job?.cancel()
        job = null
        if (!startNext) {
            summaryState.value = (stats?.takeIf { it.chapter == chapter }?.toSummary(chapterDone = true))
                ?: InterviewSummary(chapter, 0, 0, 0, 0, 0, chapterDone = true)
        }
        stats = null
        scope.launch {
            repository.markChapterDone(book.id, chapter)
            // 다음에 쓸 챕터로 넘어간다. ‘다음 챕터’면 바로 인터뷰를 이어 간다.
            val following = nextChapterAfter(book.id, chapter)
            if (following != null) repository.setCurrentChapter(book.id, following)
            val next = following.takeIf { startNext }
            if (next != null) {
                launchRun(book.id, next, intro = "좋아요. 이번에는 ‘${titleOf(next)}’ 이야기를 해 볼까요?")
            } else {
                // 정차 중이면 같은 화면에 남아 원고를 볼 수 있게, 다 채운 상태로 둔다.
                state.update { it.copy(phase = InterviewPhase.IDLE, chapter = chapter, chapterFull = true) }
            }
            eventFlow.tryEmit(InterviewEvent.ChapterFinished(chapter, next))
        }
    }

    private suspend fun run(book: Long, chapter: Int, intro: String?) {
        var prompt: String? = null
        repository.observeProgress(book, chapter).first().let { stats = SessionStats(chapter, System.currentTimeMillis(), it.answered, it.total) }
        intro?.let { say(chapter, it, null) }
        while (true) {
            val progress = repository.observeProgress(book, chapter).first()
            val question = progress.nextQuestion
            if (question == null) {
                if (!listenWhenFull(book, chapter)) return
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
            when (val command = VoiceCommand.parseCommand(heard.text)) {
                null -> Unit
                VoiceCommand.STOP -> {
                    eventFlow.tryEmit(InterviewEvent.StopRequested)
                    return
                }
                else -> {
                    // 명령은 원고에 넣지 않는다. 같은 질문을 다시 기다린다.
                    eventFlow.tryEmit(InterviewEvent.Command(command, heard.text))
                    prompt = text
                    continue
                }
            }
            repository.saveAnswer(book, question, heard.text, heard.durationSec)
            stats?.let {
                it.sentences += ListenSession.splitSentences(heard.text).size
                it.newAnswers++
            }
            Log.i(TAG, "저장: ${question.id} ${heard.text}")

            // 방금 이야기를 받아 되묻는 후속 질문(다음 질문으로 이어진다).
            val next = repository.observeProgress(book, chapter).first().nextQuestion
            prompt = next?.let { followUps.followUp(question, heard.text, it) }
        }
    }

    /**
     * 질문을 모두 채운 뒤: 안내하고 계속 듣는다. 더 말하면 덧붙인 이야기로 저장한다.
     * @return 계속 들을지. “여기까지”를 들으면 챕터를 마무리하고 false.
     */
    private suspend fun listenWhenFull(book: Long, chapter: Int): Boolean {
        val announce = fullPrompt(chapter)
        if (!state.value.chapterFull) say(chapter, announce, null, prompt = announce, full = true)
        state.value = InterviewUi(InterviewPhase.LISTENING, chapter, announce, chapterFull = true)
        val heard = input.listen(AutographyRepository.extraQuestionId(chapter)) { level ->
            state.update { it.copy(level = level) }
        }
        if (heard !is Heard.Text) return true
        when (val command = VoiceCommand.parseCommand(heard.text)) {
            null -> Unit
            VoiceCommand.STOP -> {
                finishChapter(startNext = false)
                return false
            }
            else -> {
                eventFlow.tryEmit(InterviewEvent.Command(command, heard.text))
                return true
            }
        }
        repository.saveExtra(book, chapter, heard.text, heard.durationSec)
        stats?.let { it.sentences += ListenSession.splitSentences(heard.text).size }
        Log.i(TAG, "덧붙임 저장: $chapter ${heard.text}")
        return true
    }

    private suspend fun say(chapter: Int, text: String, question: QuestionEntity?, prompt: String? = null, full: Boolean = false) {
        state.value = InterviewUi(InterviewPhase.ASKING, chapter, prompt, question, chapterFull = full)
        audio.ducking { output.speak(text.replace('\n', ' ')) }
    }

    /** 다음에 쓸 챕터: 뒤쪽부터, 질문 은행이 있고 아직 마무리하지 않은 챕터. 모두 마쳤으면 null. */
    private suspend fun nextChapterAfter(book: Long, chapter: Int): Int? {
        val done = repository.doneChapters(book) + chapter
        val order = ((chapter + 1)..LAST_CHAPTER) + (1 until chapter)
        return order.firstOrNull { it !in done && repository.questionCount(it) > 0 }
    }

    companion object {
        private const val TAG = "InterviewSession"

        /** 책을 읽기 전 기본으로 보일 챕터(2장 ‘첫 차’, 첫 실행의 기본 선택). */
        const val FIRST_WRITING_CHAPTER = 2
        const val LAST_CHAPTER = 12

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
