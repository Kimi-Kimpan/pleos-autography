package com.pleosautography.app.domain.listen

import com.pleosautography.app.data.demo.DemoData
import com.pleosautography.app.data.repo.AutographyRepository
import com.pleosautography.app.data.speech.CaptionSpeechOutput
import com.pleosautography.app.data.speech.SpeechOutput
import com.pleosautography.app.domain.interview.InterviewSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 낭독할 문장 한 개. [answerId]가 null이면 예시 원고(저장된 답변이 없을 때)다. */
data class ListenSentence(val answerId: Long?, val text: String)

/** 말로 고친 자리: [sentence]번째 문장의 [wrong]을 [fixed]로 바꿨다. */
data class AppliedFix(val sentence: Int, val wrong: String, val fixed: String)

/** 고치기 대화 한 번(사용자 말 → 오토 대답). */
data class FixExchange(val userSaid: String, val otoReply: String)

/**
 * 원고 듣기 화면이 보여 줄 상태.
 * @param index 지금 읽는(또는 멈춘) 문장.
 */
data class ListenUi(
    val chapter: Int = 0,
    val sentences: List<ListenSentence> = emptyList(),
    val index: Int = 0,
    val playing: Boolean = false,
    val finished: Boolean = false,
    val fix: AppliedFix? = null,
    val exchange: FixExchange? = null,
) {
    val current: ListenSentence? get() = sentences.getOrNull(index)
    val previous: ListenSentence? get() = sentences.getOrNull(index - 1)
    val totalMs: Long get() = sentences.sumOf { CaptionSpeechOutput.estimateMs(it.text) }
    val elapsedMs: Long get() = sentences.take(if (finished) sentences.size else index).sumOf { CaptionSpeechOutput.estimateMs(it.text) }
}

/**
 * 내 원고 듣기·고치기(마일스톤 7의 시연판). 지금 챕터의 답변을 문장 단위로 읽고,
 * “방금 거기 고치자. A가 아니라 B였어.”를 들으면 읽던 자리 근처에서 A를 찾아 B로 고쳐 저장한다(editedAt).
 * 소리는 [SpeechOutput]이 낸다(시연은 읽는 시간만 기다리는 [CaptionSpeechOutput]).
 */
class ListenSession(
    private val scope: CoroutineScope,
    private val repository: AutographyRepository,
    private val output: SpeechOutput,
) {
    private val state = MutableStateFlow(ListenUi())
    val ui: StateFlow<ListenUi> = state.asStateFlow()

    private var job: Job? = null
    private var active = false

    /** 화면이 열리면 지금 챕터 원고를 처음부터 읽는다. */
    fun start() {
        if (active) return
        active = true
        job = scope.launch {
            state.value = load()
            playFrom(0)
        }
    }

    fun stop() {
        active = false
        job?.cancel()
        job = null
        state.value = ListenUi()
    }

    /** 읽는 중이면 멈춘다(플레이어 시트가 열릴 때). */
    fun pause() {
        if (!active || !state.value.playing) return
        job?.cancel()
        state.update { it.copy(playing = false) }
    }

    /** 일시정지·이어 듣기. 다 읽었으면 처음부터. */
    fun togglePlay() {
        if (!active) return
        if (state.value.playing) {
            job?.cancel()
            state.update { it.copy(playing = false) }
        } else {
            val from = if (state.value.finished) 0 else state.value.index
            restartFrom(from)
        }
    }

    /** 다음 문단(다음 답변)의 첫 문장으로. */
    fun nextParagraph() {
        val ui = state.value
        if (!active || ui.sentences.isEmpty()) return
        val currentAnswer = ui.current?.answerId
        val next = (ui.index + 1 until ui.sentences.size).firstOrNull { ui.sentences[it].answerId != currentAnswer || currentAnswer == null }
        if (next != null) restartFrom(next)
    }

    /** “방금 거기 고치자” 뒤에 들린 정정. 읽던 문장부터 앞쪽, 그다음 뒤쪽으로 고칠 말을 찾는다. */
    fun applyFix(said: String) {
        if (!active) return
        job?.cancel()
        job = scope.launch {
            val ui = state.value
            val shown = said.replace(Regex("(고치자[.!]?)\\s*"), "$1\n").trim()
            val parsed = FixParser.parse(said)
            if (parsed == null) {
                reply(FixExchange(shown, "어느 부분을 고칠지\n한 번 더 말씀해 주세요."))
                playFrom(ui.index)
                return@launch
            }
            val order = (ui.index downTo 0) + (ui.index + 1 until ui.sentences.size)
            val target = order.firstOrNull { parsed.wrong in ui.sentences[it].text }
            if (target == null) {
                reply(FixExchange(shown, "‘${parsed.wrong}’${subjectParticle(parsed.wrong)} 원고에서\n찾지 못했어요."))
                playFrom(ui.index)
                return@launch
            }
            val sentence = ui.sentences[target]
            val fixedText = sentence.text.replaceFirst(parsed.wrong, parsed.fixed)
            val sentences = ui.sentences.toMutableList().also { it[target] = sentence.copy(text = fixedText) }
            // 같은 답변의 문장을 다시 이어 저장한다(원래 띄어쓰기 한 칸).
            sentence.answerId?.let { id ->
                repository.updateTranscript(id, sentences.filter { it.answerId == id }.joinToString(" ") { it.text })
            }
            val phrase = phraseAround(fixedText, parsed.fixed)
            state.update {
                it.copy(sentences = sentences, index = target, playing = false, finished = false, fix = AppliedFix(target, parsed.wrong, parsed.fixed))
            }
            reply(FixExchange(shown, "‘$phrase’${directionParticle(phrase)} 고쳤어요.\n이어서 읽어 드릴게요."))
            playFrom(target)
        }
    }

    private suspend fun reply(exchange: FixExchange) {
        state.update { it.copy(exchange = exchange, playing = false) }
        output.speak(exchange.otoReply.replace('\n', ' '))
    }

    private fun restartFrom(index: Int) {
        job?.cancel()
        job = scope.launch { playFrom(index) }
    }

    private suspend fun playFrom(start: Int) {
        var i = start
        while (i < state.value.sentences.size) {
            state.update { it.copy(index = i, playing = true, finished = false) }
            output.speak(state.value.sentences[i].text)
            i++
        }
        state.update { it.copy(playing = false, finished = true) }
    }

    /** 지금 챕터에 답변이 있으면 그 챕터, 없으면 가장 최근에 답한 챕터. 하나도 없으면 예시 원고. */
    private suspend fun load(): ListenUi {
        val book = repository.activeBook() ?: return example(InterviewSession.FIRST_WRITING_CHAPTER)
        var chapter = book.currentChapter
        var answers = repository.answers(book.id, chapter)
        if (answers.isEmpty()) {
            val latest = repository.bookAnswers(book.id).maxByOrNull { it.createdAt } ?: return example(chapter)
            chapter = latest.chapterNumber
            answers = repository.answers(book.id, chapter)
        }
        return ListenUi(chapter = chapter, sentences = answers.flatMap { answer -> splitSentences(answer.transcript).map { ListenSentence(answer.id, it) } })
    }

    private fun example(chapter: Int) =
        ListenUi(chapter = chapter, sentences = DemoData.manuscript.flatMap { p -> splitSentences(p.text).map { ListenSentence(null, it) } })

    companion object {
        fun splitSentences(text: String): List<String> =
            text.trim().split(Regex("(?<=[.!?…])\\s+")).filter { it.isNotBlank() }

        /** 고친 말과 그 뒤 한 낱말(“1987년 겨울”). */
        fun phraseAround(sentence: String, fixed: String): String {
            val at = sentence.indexOf(fixed)
            if (at < 0) return fixed
            val rest = sentence.substring(at + fixed.length)
            val nextWord = if (rest.startsWith(" ")) rest.trim().split(' ').firstOrNull()?.trimEnd(',', '.', '!', '?') else null
            return if (nextWord.isNullOrEmpty()) fixed else "$fixed $nextWord"
        }

        /** “로/으로”: 받침이 없거나 ㄹ 받침이면 “로”. */
        fun directionParticle(word: String): String {
            val last = word.lastOrNull { it in '가'..'힣' } ?: return "로"
            val jong = (last - '가') % 28
            return if (jong == 0 || jong == 8) "로" else "으로"
        }

        private fun subjectParticle(word: String): String {
            val last = word.lastOrNull { it in '가'..'힣' } ?: return "를"
            return if ((last - '가') % 28 != 0) "을" else "를"
        }
    }
}

/** “A가 아니라 B였어”에서 고칠 말(A)과 바른 말(B)을 꺼낸다. */
object FixParser {
    data class Fix(val wrong: String, val fixed: String)

    private val pattern = Regex("([^\\s,.!?]+)\\s*아니라\\s*([^\\s,.!?]+)")
    private val subjectEndings = listOf("이", "가")
    private val copulaEndings = listOf("이었어요", "였어요", "이었어", "였어", "이에요", "예요", "입니다", "이야", "야", "이지", "지", "요")

    fun parse(text: String): Fix? {
        val match = pattern.find(text) ?: return null
        val wrong = match.groupValues[1].stripOne(subjectEndings)
        val fixed = match.groupValues[2].stripOne(copulaEndings)
        if (wrong.isEmpty() || fixed.isEmpty() || wrong == fixed) return null
        return Fix(wrong, fixed)
    }

    private fun String.stripOne(endings: List<String>): String {
        val ending = endings.firstOrNull { endsWith(it) && length > it.length } ?: return this
        return removeSuffix(ending)
    }
}
