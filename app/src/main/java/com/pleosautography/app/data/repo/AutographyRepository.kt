package com.pleosautography.app.data.repo

import android.content.res.AssetManager
import androidx.room.withTransaction
import com.pleosautography.app.data.db.AnswerEntity
import com.pleosautography.app.data.db.AppDatabase
import com.pleosautography.app.data.db.BookEntity
import com.pleosautography.app.data.db.ChapterStateEntity
import com.pleosautography.app.data.db.QuestionEntity
import com.pleosautography.app.data.db.SettingsEntity
import com.pleosautography.app.data.speech.DemoScript
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 챕터 진행 상황.
 * @param nextQuestion 아직 답하지 않은 첫 질문. 다 답했으면 null(챕터 완성).
 */
data class ChapterProgress(
    val chapterNumber: Int,
    val answered: Int,
    val total: Int,
    val nextQuestion: QuestionEntity?,
    val lastAnswer: AnswerEntity?,
) {
    /** 진행률: 답변 수 / 질문 수(CLAUDE.md 인터뷰 엔진). */
    val progress: Float get() = if (total == 0) 0f else answered.toFloat() / total

    /** 지금 묻는(또는 다음에 물을) 질문 번호. */
    val questionNumber: Int get() = (answered + 1).coerceAtMost(total)

    val complete: Boolean get() = total > 0 && answered >= total
}

/** 책 전체 통계. */
data class BookStats(val doneChapters: Int, val stories: Int, val conversations: Int, val pages: Int)

/** 설정 목록에 보일 책 한 권의 요약. */
data class BookSummary(val book: BookEntity, val doneChapters: Int, val stories: Int, val lastActivityAt: Long)

/**
 * 서재: 설정과 책 목록. [settings]가 null이면 아직 첫 실행을 마치지 않았다.
 * @param active 지금 쓰는 책. 책이 하나도 없으면 null(첫 실행 화면).
 */
data class Library(
    val settings: SettingsEntity?,
    val books: List<BookSummary>,
) {
    val active: BookEntity? get() = books.firstOrNull { it.book.id == settings?.activeBookId }?.book ?: books.lastOrNull()?.book
}

/** 질문 은행·책·답변 저장소. */
@OptIn(ExperimentalCoroutinesApi::class)
class AutographyRepository(private val db: AppDatabase, private val assets: AssetManager, scope: CoroutineScope) {

    /** 서재 상태. 아직 읽기 전이면 null. */
    val library: StateFlow<Library?> = combine(
        db.settings().observe(),
        db.books().observeAll(),
        db.chapters().observeAll(),
        db.answers().observeAll(),
    ) { settings, books, states, answers ->
        val doneByBook = states.groupBy { it.bookId }
        val answersByBook = answers.groupBy { it.bookId }
        Library(
            settings = settings,
            books = books.map { book ->
                val own = answersByBook[book.id].orEmpty()
                BookSummary(
                    book = book,
                    doneChapters = doneByBook[book.id].orEmpty().size,
                    stories = own.filterNot { it.isExtra }.map { it.questionId }.distinct().size,
                    lastActivityAt = maxOf(book.createdAt, own.maxOfOrNull { it.createdAt } ?: 0L),
                )
            },
        )
    }.stateIn(scope, SharingStarted.Eagerly, null)

    /** 지금 쓰는 책 id(없으면 null). */
    val activeBookId: Flow<Long?> = library.map { it?.active?.id }

    fun activeBook(): BookEntity? = library.value?.active

    /** assets/questions 폴더의 JSON을 DB에 넣는다(실행할 때마다 덮어써 질문 은행이 바뀌어도 맞춘다). */
    suspend fun syncQuestions() = withContext(Dispatchers.IO) {
        val files = assets.list(Const.QUESTION_DIR).orEmpty().filter { it.endsWith(".json") }
        val questions = files.flatMap { file ->
            val json = JSONObject(assets.open("${Const.QUESTION_DIR}/$file").bufferedReader().use { it.readText() })
            val chapter = json.getInt("chapter")
            val list = json.getJSONArray("questions")
            (0 until list.length()).map { i ->
                val q = list.getJSONObject(i)
                QuestionEntity(q.getString("id"), chapter, i + 1, q.getString("text"), q.optString("followUpHint"))
            }
        }
        db.questions().insertAll(questions)
    }

    // ── 책·설정 ────────────────────────────────────────────

    /**
     * 첫 실행(또는 새 자서전)을 마치면 책을 만들고 지금 쓰는 책으로 둔다.
     * @param demoSeed 시연용 예시 답변(디버그 빌드의 첫 책에만 준다).
     */
    suspend fun createBook(title: String, ownerName: String, callName: String, firstChapter: Int, demoSeed: Map<String, String>? = null): Long =
        withContext(Dispatchers.IO) {
            syncQuestionsIfEmpty()
            db.withTransaction {
                val now = System.currentTimeMillis()
                val id = db.books().insert(BookEntity(title = title, ownerCallName = callName, createdAt = now, currentChapter = firstChapter))
                val settings = db.settings().get()
                db.settings().upsert(
                    settings?.copy(ownerName = ownerName, callName = callName, activeBookId = id)
                        ?: SettingsEntity(ownerName = ownerName, callName = callName, activeBookId = id),
                )
                if (demoSeed != null) seedAnswers(id, demoSeed, now)
                id
            }
        }

    /**
     * 시연용 서재(디버그 빌드, demo.sh --reset): 예시 답변이 든 책(1장 완성, ‘첫 차’ 5편)과
     * 지난번에 쓰던 다른 책 한 권. 이미 책이 있으면 아무것도 하지 않는다.
     */
    suspend fun setupDemoLibrary(seed: Map<String, String>) = withContext(Dispatchers.IO) {
        syncQuestionsIfEmpty()
        db.withTransaction {
            if (db.books().all().isNotEmpty()) return@withTransaction
            val now = System.currentTimeMillis()
            val other = db.books().insert(
                BookEntity(title = "우리 가게 이야기", ownerCallName = DEMO_CALL_NAME, createdAt = now - 21 * Const.DAY_MS, currentChapter = 2),
            )
            db.chapters().upsert(ChapterStateEntity(other, 1, now - 21 * Const.DAY_MS))
            val main = db.books().insert(
                BookEntity(title = DEMO_TITLE, ownerCallName = DEMO_CALL_NAME, createdAt = now - 30 * Const.DAY_MS, currentChapter = 2),
            )
            seedAnswers(main, seed, now)
            db.settings().upsert(SettingsEntity(ownerName = DEMO_OWNER, callName = DEMO_CALL_NAME, activeBookId = main))
        }
    }

    /** 예시 답변을 넣고, 질문을 다 채운 챕터는 완성으로 기록한다. */
    private suspend fun seedAnswers(book: Long, seed: Map<String, String>, now: Long) {
        seed.entries.forEachIndexed { index, (questionId, text) ->
            val chapter = questionId.substringBefore('-').toInt()
            db.answers().insert(AnswerEntity(bookId = book, chapterNumber = chapter, questionId = questionId, transcript = text, createdAt = now - (seed.size - index) * Const.DAY_MS, durationSec = 40))
        }
        seed.keys.map { it.substringBefore('-').toInt() }.distinct().forEach { chapter ->
            val questions = db.questions().chapter(chapter)
            if (questions.isNotEmpty() && questions.all { it.id in seed }) {
                db.chapters().upsert(ChapterStateEntity(book, chapter, now - seed.size * Const.DAY_MS))
            }
        }
    }

    private suspend fun syncQuestionsIfEmpty() {
        if (db.questions().count() == 0) syncQuestions()
    }

    suspend fun switchBook(id: Long) = updateSettings { it.copy(activeBookId = id) }

    suspend fun renameBook(id: Long, title: String) = withContext(Dispatchers.IO) {
        db.books().get(id)?.let { db.books().update(it.copy(title = title)) }
    }

    /** 책을 지운다(답변·챕터 기록 포함). 지금 쓰는 책이었으면 남은 책 중 가장 최근 책으로 바꾼다. */
    suspend fun deleteBook(id: Long) = withContext(Dispatchers.IO) {
        db.withTransaction {
            db.answers().deleteBook(id)
            db.chapters().deleteBook(id)
            db.books().delete(id)
            val settings = db.settings().get() ?: return@withTransaction
            if (settings.activeBookId == id) {
                db.settings().upsert(settings.copy(activeBookId = db.books().all().lastOrNull()?.id))
            }
        }
    }

    suspend fun requestPublish(id: Long) = withContext(Dispatchers.IO) {
        db.books().get(id)?.let { db.books().update(it.copy(publishRequestedAt = System.currentTimeMillis())) }
    }

    suspend fun setCurrentChapter(book: Long, chapter: Int) = withContext(Dispatchers.IO) {
        db.books().get(book)?.let { db.books().update(it.copy(currentChapter = chapter)) }
    }

    suspend fun updateSettings(change: (SettingsEntity) -> SettingsEntity) = withContext(Dispatchers.IO) {
        db.settings().get()?.let { db.settings().upsert(change(it)) }
    }

    // ── 챕터·답변 ──────────────────────────────────────────

    fun observeProgress(book: Long, chapter: Int): Flow<ChapterProgress> =
        combine(db.questions().observeChapter(chapter), db.answers().observeChapter(book, chapter)) { questions, answers ->
            val answeredIds = answers.map { it.questionId }.toSet()
            ChapterProgress(
                chapterNumber = chapter,
                answered = questions.count { it.id in answeredIds },
                total = questions.size,
                nextQuestion = questions.firstOrNull { it.id !in answeredIds },
                lastAnswer = answers.lastOrNull(),
            )
        }

    /** 지금 쓰는 책의 지금 챕터 진행 상황. */
    val currentProgress: Flow<ChapterProgress?> = library
        .map { lib -> lib?.active?.let { it.id to it.currentChapter } }
        .flatMapLatest { key -> key?.let { (book, chapter) -> observeProgress(book, chapter) } ?: flowOf(null) }

    suspend fun questionCount(chapter: Int): Int = withContext(Dispatchers.IO) { db.questions().chapter(chapter).size }

    /** 마무리한 챕터 번호들. */
    suspend fun doneChapters(book: Long): Set<Int> = withContext(Dispatchers.IO) { db.chapters().done(book).toSet() }

    /** 챕터를 다 채운 뒤 덧붙인 이야기. 진행률에는 들어가지 않는다. */
    suspend fun saveExtra(book: Long, chapter: Int, transcript: String, durationSec: Int): Long = withContext(Dispatchers.IO) {
        db.answers().insert(
            AnswerEntity(
                bookId = book,
                chapterNumber = chapter,
                questionId = extraQuestionId(chapter),
                transcript = transcript,
                createdAt = System.currentTimeMillis(),
                durationSec = durationSec,
            ),
        )
    }

    suspend fun saveAnswer(book: Long, question: QuestionEntity, transcript: String, durationSec: Int): Long =
        withContext(Dispatchers.IO) {
            db.answers().insert(
                AnswerEntity(
                    bookId = book,
                    chapterNumber = question.chapterNumber,
                    questionId = question.id,
                    transcript = transcript,
                    createdAt = System.currentTimeMillis(),
                    durationSec = durationSec,
                ),
            )
        }

    /** 챕터의 답변(원고 문단). 덧붙인 이야기도 포함한다. */
    fun observeAnswers(book: Long, chapter: Int): Flow<List<AnswerEntity>> = db.answers().observeChapter(book, chapter)

    suspend fun answers(book: Long, chapter: Int): List<AnswerEntity> = withContext(Dispatchers.IO) { db.answers().chapter(book, chapter) }

    suspend fun bookAnswers(book: Long): List<AnswerEntity> = withContext(Dispatchers.IO) { db.answers().book(book) }

    /** 원고 문장을 고친다(말로 고치기). 고친 때를 남긴다. */
    suspend fun updateTranscript(id: Long, transcript: String) = withContext(Dispatchers.IO) {
        db.answers().updateTranscript(id, transcript, System.currentTimeMillis())
    }

    /** 조건에 맞는 답변을 지운다(예전 빌드가 음성 명령을 답변으로 저장한 것 정리). 지운 수를 돌려준다. */
    suspend fun deleteAnswersWhere(predicate: (AnswerEntity) -> Boolean): Int = withContext(Dispatchers.IO) {
        val targets = db.answers().all().filter(predicate)
        targets.forEach { db.answers().delete(it.id) }
        targets.size
    }

    suspend fun deleteAnswer(id: Long) = withContext(Dispatchers.IO) { db.answers().delete(id) }

    /** 지운 문단을 되돌린다(같은 id로 다시 넣는다). */
    suspend fun restoreAnswer(answer: AnswerEntity) = withContext(Dispatchers.IO) { db.answers().insert(answer) }

    suspend fun markChapterDone(book: Long, chapter: Int) = withContext(Dispatchers.IO) {
        db.chapters().upsert(ChapterStateEntity(book, chapter, System.currentTimeMillis()))
    }

    /** 책 전체 상태: 마무리한 챕터, 챕터별 이야기 수, 통계. */
    fun observeBook(book: Long): Flow<BookContent> =
        combine(db.answers().observeAll(), db.chapters().observeAll()) { all, states ->
            val answers = all.filter { it.bookId == book }
            val stories = answers.filterNot { it.isExtra }
            BookContent(
                done = states.filter { it.bookId == book }.map { it.chapterNumber }.toSet(),
                stories = stories.groupBy { it.chapterNumber }.mapValues { (_, list) -> list.map { it.questionId }.distinct().size },
                stats = BookStats(
                    doneChapters = states.count { it.bookId == book },
                    stories = stories.map { it.questionId }.distinct().size,
                    conversations = answers.size,
                    pages = (answers.sumOf { it.transcript.length } / Const.CHARS_PER_PAGE).coerceAtLeast(if (answers.isEmpty()) 0 else 1),
                ),
            )
        }

    companion object {
        fun extraQuestionId(chapter: Int) = "%02d%s".format(chapter, DemoScript.EXTRA_SUFFIX)

        // 시연용 서재의 이름(실명이 아닌 가림 표시).
        const val DEMO_OWNER = "김○○"
        const val DEMO_CALL_NAME = "○○님"
        const val DEMO_TITLE = "김○○의 자서전"
    }

    private object Const {
        const val QUESTION_DIR = "questions"
        const val DAY_MS = 24 * 60 * 60 * 1000L
        const val CHARS_PER_PAGE = 600
    }
}

/** 책 한 권의 내용 요약. */
data class BookContent(val done: Set<Int>, val stories: Map<Int, Int>, val stats: BookStats)

/** 챕터를 다 채운 뒤 덧붙인 이야기인지. */
val AnswerEntity.isExtra: Boolean get() = questionId.endsWith(DemoScript.EXTRA_SUFFIX)
