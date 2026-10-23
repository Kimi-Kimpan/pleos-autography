package com.pleosautography.app.data.repo

import android.content.res.AssetManager
import com.pleosautography.app.data.db.AnswerEntity
import com.pleosautography.app.data.db.AppDatabase
import com.pleosautography.app.data.db.QuestionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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

/** 질문 은행과 답변 저장소. */
class AutographyRepository(private val db: AppDatabase, private val assets: AssetManager) {

    /** 처음 실행이면 assets/questions 폴더의 JSON을 DB에 넣는다. */
    suspend fun seedQuestionsIfNeeded() = withContext(Dispatchers.IO) {
        if (db.questions().count() > 0) return@withContext
        val files = assets.list(QUESTION_DIR).orEmpty().filter { it.endsWith(".json") }
        val questions = files.flatMap { file ->
            val json = JSONObject(assets.open("$QUESTION_DIR/$file").bufferedReader().use { it.readText() })
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
            db.answers().insert(AnswerEntity(0, chapter, questionId, text, now - (seed.size - index) * DAY_MS, 40))
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

    private companion object {
        const val QUESTION_DIR = "questions"
        const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
