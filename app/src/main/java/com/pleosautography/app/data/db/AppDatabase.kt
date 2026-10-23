package com.pleosautography.app.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** 질문 은행(assets/questions 폴더의 JSON에서 시드한다). */
@Entity(tableName = "question", indices = [Index("chapterNumber")])
data class QuestionEntity(
    @PrimaryKey val id: String,
    val chapterNumber: Int,
    val order: Int,
    val text: String,
    val followUpHint: String,
)

/** 운전자의 답변. 받아 적는 즉시 저장한다(데이터 유실 없음). */
@Entity(tableName = "answer", indices = [Index("chapterNumber"), Index("questionId")])
data class AnswerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chapterNumber: Int,
    val questionId: String,
    val transcript: String,
    val createdAt: Long,
    val durationSec: Int,
    val editedAt: Long? = null,
)

@Dao
interface QuestionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<QuestionEntity>)

    @Query("SELECT * FROM question WHERE chapterNumber = :chapter ORDER BY `order`")
    fun observeChapter(chapter: Int): Flow<List<QuestionEntity>>

    @Query("SELECT COUNT(*) FROM question")
    suspend fun count(): Int
}

@Dao
interface AnswerDao {
    @Insert
    suspend fun insert(answer: AnswerEntity): Long

    @Query("SELECT * FROM answer WHERE chapterNumber = :chapter ORDER BY createdAt")
    fun observeChapter(chapter: Int): Flow<List<AnswerEntity>>

    @Query("SELECT COUNT(*) FROM answer")
    suspend fun count(): Int
}

/**
 * 기기 안의 로컬 DB(서버 없음). Book·Chapter·Settings 테이블은 정차 화면(마일스톤 2 묶음 C·D)과 함께 붙인다.
 */
@Database(entities = [QuestionEntity::class, AnswerEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun questions(): QuestionDao
    abstract fun answers(): AnswerDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "autography.db").build()
    }
}
