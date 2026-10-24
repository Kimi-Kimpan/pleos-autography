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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

/** 챕터를 마무리한 기록. 행이 있으면 그 챕터는 완성(DONE)이다. */
@Entity(tableName = "chapter_state")
data class ChapterStateEntity(
    @PrimaryKey val chapterNumber: Int,
    val completedAt: Long,
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

    @Query("SELECT * FROM answer")
    fun observeAll(): Flow<List<AnswerEntity>>
}

@Dao
interface ChapterStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: ChapterStateEntity)

    @Query("SELECT chapterNumber FROM chapter_state")
    fun observeDone(): Flow<List<Int>>
}

/**
 * 기기 안의 로컬 DB(서버 없음). Book·Settings 테이블은 정차 화면(마일스톤 2 묶음 C·D)과 함께 붙인다.
 */
@Database(
    entities = [QuestionEntity::class, AnswerEntity::class, ChapterStateEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun questions(): QuestionDao
    abstract fun answers(): AnswerDao
    abstract fun chapters(): ChapterStateDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "autography.db")
                .addMigrations(MIGRATION_1_2)
                .build()

        /** 1 → 2: 챕터 완성 기록 테이블. 기존 답변은 그대로 둔다. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `chapter_state` (`chapterNumber` INTEGER NOT NULL, `completedAt` INTEGER NOT NULL, PRIMARY KEY(`chapterNumber`))")
            }
        }
    }
}
