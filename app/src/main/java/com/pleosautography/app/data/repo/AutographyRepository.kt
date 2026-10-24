package com.pleosautography.app.data.repo

import android.content.res.AssetManager
import com.pleosautography.app.data.db.AnswerEntity
import com.pleosautography.app.data.db.AppDatabase
import com.pleosautography.app.data.db.ChapterStateEntity
import com.pleosautography.app.data.db.QuestionEntity
import com.pleosautography.app.data.speech.DemoScript
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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

/** 질문 은행과 답변 저장소. */
class AutographyRepository(private val db: AppDatabase, private val assets: AssetManager) {

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

    /** 시연용: 답변이 하나도 없으면 예시 답변을 넣는다(디버그 빌드에서만 부른다). */
    suspend fun seedDemoAnswersIfEmpty(seed: Map<String, String>) = withContext(Dispatchers.IO) {
        if (db.answers().count() > 0) return@withContext
        val now = System.currentTimeMillis()
        seed.entries.forEachIndexed { index, (questionId, text) ->
            val chapter = questionId.substringBefore('-').toInt()
            db.answers().insert(AnswerEntity(0, chapter, questionId, text, now - (seed.size - index) * Const.DAY_MS, 40))
        }
    }

    fun observeProgress(chapter: Int): Flow<ChapterProgress> =
        combine(db.questions().observeChapter(chapter), db.answers().observeChapter(chapter)) { questions, answers ->
            val answeredIds = answers.map { it.questionId }.toSet()
            ChapterProgress(
                chapterNumber = chapter,
                answered = questions.count { it.id in answeredIds },
                total = questions.size,
                nextQuestion = questions.firstOrNull { it.id !in answeredIds },
                lastAnswer = answers.lastOrNull(),
            )
        }

    /** 챕터를 다 채운 뒤 덧붙인 이야기. 진행률에는 들어가지 않는다. */
    suspend fun saveExtra(chapter: Int, transcript: String, durationSec: Int): Long = withContext(Dispatchers.IO) {
        db.answers().insert(
            AnswerEntity(
                chapterNumber = chapter,
                questionId = extraQuestionId(chapter),
                transcript = transcript,
                createdAt = System.currentTimeMillis(),
                durationSec = durationSec,
            ),
        )
    }

    /** 챕터의 답변(원고 문단). 덧붙인 이야기도 포함한다. */
    fun observeAnswers(chapter: Int): Flow<List<AnswerEntity>> = db.answers().observeChapter(chapter)

    /** 마무리한 챕터 번호들. */
    fun observeDoneChapters(): Flow<Set<Int>> = db.chapters().observeDone().map { it.toSet() }

    suspend fun markChapterDone(chapter: Int) = withContext(Dispatchers.IO) {
        db.chapters().upsert(ChapterStateEntity(chapter, System.currentTimeMillis()))
    }

    /** 챕터별 이야기 수(답한 질문 수, 덧붙인 이야기는 빼고). */
    fun observeStoriesByChapter(): Flow<Map<Int, Int>> = db.answers().observeAll().map { answers ->
        answers.filterNot { it.questionId.endsWith(DemoScript.EXTRA_SUFFIX) }
            .groupBy { it.chapterNumber }
            .mapValues { (_, list) -> list.map { it.questionId }.distinct().size }
    }

    /** 책 전체 통계(완성 화면). 원고 쪽수는 한 쪽 600자로 어림한다. */
    fun observeBookStats(): Flow<BookStats> =
        combine(db.answers().observeAll(), observeDoneChapters()) { answers, done ->
            BookStats(
                doneChapters = done.size,
                stories = answers.map { it.questionId }.distinct().size,
                conversations = answers.size,
                pages = (answers.sumOf { it.transcript.length } / Const.CHARS_PER_PAGE).coerceAtLeast(if (answers.isEmpty()) 0 else 1),
            )
        }

    suspend fun saveAnswer(question: QuestionEntity, transcript: String, durationSec: Int): Long =
        withContext(Dispatchers.IO) {
            db.answers().insert(
                AnswerEntity(
                    chapterNumber = question.chapterNumber,
                    questionId = question.id,
                    transcript = transcript,
                    createdAt = System.currentTimeMillis(),
                    durationSec = durationSec,
                ),
            )
        }

    companion object {
        fun extraQuestionId(chapter: Int) = "%02d%s".format(chapter, DemoScript.EXTRA_SUFFIX)
    }

    private object Const {
        const val QUESTION_DIR = "questions"
        const val DAY_MS = 24 * 60 * 60 * 1000L
        const val CHARS_PER_PAGE = 600
    }
}
