package com.pleosautography.app.data.db

import android.content.Context
import androidx.room.ColumnInfo
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
import androidx.room.Update
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

/**
 * 자서전 한 권. 여러 권을 쓸 수 있고, 지금 쓰는 책은 [SettingsEntity.activeBookId]다.
 * @param currentChapter 지금 쓰는 챕터 번호.
 * @param publishRequestedAt 출판 신청을 접수한 때(MVP는 접수 안내만).
 */
@Entity(tableName = "book")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val ownerCallName: String,
    val createdAt: Long,
    val currentChapter: Int,
    val publishRequestedAt: Long? = null,
)

/**
 * 앱 설정(한 줄). 첫 실행을 마치면 생긴다.
 * @param ownerName 첫 실행에서 적은 이름(제목 추천에 쓴다). [callName]은 오토가 부르는 이름.
 * @param demoMode 시연 모드(심사·시연용, release에도 있다): 샘플 자서전과 데모 패널을 쓴다.
 */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = SINGLE_ROW,
    val ownerName: String,
    val callName: String,
    val proactiveTalk: Boolean = true,
    val chimeOn: Boolean = true,
    val radioPublic: Boolean = false,
    val activeBookId: Long? = null,
    @ColumnInfo(defaultValue = "0") val demoMode: Boolean = false,
) {
    companion object {
        const val SINGLE_ROW = 1
    }
}

/** 운전자의 답변. 받아 적는 즉시 저장한다(데이터 유실 없음). */
@Entity(tableName = "answer", indices = [Index("chapterNumber"), Index("questionId"), Index("bookId")])
data class AnswerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(defaultValue = "1") val bookId: Long,
    val chapterNumber: Int,
    val questionId: String,
    val transcript: String,
    val createdAt: Long,
    val durationSec: Int,
    val editedAt: Long? = null,
)

/** 챕터를 마무리한 기록. 행이 있으면 그 책의 그 챕터는 완성(DONE)이다. */
@Entity(tableName = "chapter_state", primaryKeys = ["bookId", "chapterNumber"])
data class ChapterStateEntity(
    val bookId: Long,
    val chapterNumber: Int,
    val completedAt: Long,
)

@Dao
interface QuestionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<QuestionEntity>)

    @Query("SELECT * FROM question WHERE chapterNumber = :chapter ORDER BY `order`")
    fun observeChapter(chapter: Int): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM question WHERE chapterNumber = :chapter ORDER BY `order`")
    suspend fun chapter(chapter: Int): List<QuestionEntity>

    @Query("SELECT COUNT(*) FROM question")
    suspend fun count(): Int
}

@Dao
interface BookDao {
    @Insert
    suspend fun insert(book: BookEntity): Long

    @Update
    suspend fun update(book: BookEntity)

    @Query("SELECT * FROM book WHERE id = :id")
    suspend fun get(id: Long): BookEntity?

    @Query("SELECT * FROM book ORDER BY createdAt")
    fun observeAll(): Flow<List<BookEntity>>

    @Query("SELECT * FROM book ORDER BY createdAt")
    suspend fun all(): List<BookEntity>

    @Query("DELETE FROM book WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface SettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: SettingsEntity)

    @Query("SELECT * FROM settings WHERE id = ${SettingsEntity.SINGLE_ROW}")
    fun observe(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = ${SettingsEntity.SINGLE_ROW}")
    suspend fun get(): SettingsEntity?
}

@Dao
interface AnswerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(answer: AnswerEntity): Long

    @Query("SELECT * FROM answer WHERE bookId = :book AND chapterNumber = :chapter ORDER BY createdAt")
    fun observeChapter(book: Long, chapter: Int): Flow<List<AnswerEntity>>

    @Query("SELECT * FROM answer WHERE bookId = :book AND chapterNumber = :chapter ORDER BY createdAt")
    suspend fun chapter(book: Long, chapter: Int): List<AnswerEntity>

    @Query("SELECT * FROM answer WHERE bookId = :book ORDER BY createdAt")
    suspend fun book(book: Long): List<AnswerEntity>

    @Query("SELECT * FROM answer WHERE id = :id")
    suspend fun get(id: Long): AnswerEntity?

    @Query("SELECT COUNT(*) FROM answer")
    suspend fun count(): Int

    @Query("SELECT * FROM answer")
    suspend fun all(): List<AnswerEntity>

    @Query("SELECT * FROM answer")
    fun observeAll(): Flow<List<AnswerEntity>>

    @Query("UPDATE answer SET transcript = :transcript, editedAt = :editedAt WHERE id = :id")
    suspend fun updateTranscript(id: Long, transcript: String, editedAt: Long)

    @Query("DELETE FROM answer WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM answer WHERE bookId = :book")
    suspend fun deleteBook(book: Long)
}

@Dao
interface ChapterStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: ChapterStateEntity)

    @Query("SELECT * FROM chapter_state")
    fun observeAll(): Flow<List<ChapterStateEntity>>

    @Query("SELECT chapterNumber FROM chapter_state WHERE bookId = :book")
    suspend fun done(book: Long): List<Int>

    @Query("DELETE FROM chapter_state WHERE bookId = :book")
    suspend fun deleteBook(book: Long)
}

/** 기기 안의 로컬 DB(서버 없음). */
@Database(
    entities = [
        QuestionEntity::class,
        BookEntity::class,
        SettingsEntity::class,
        AnswerEntity::class,
        ChapterStateEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun questions(): QuestionDao
    abstract fun books(): BookDao
    abstract fun settings(): SettingsDao
    abstract fun answers(): AnswerDao
    abstract fun chapters(): ChapterStateDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "autography.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()

        /** 1 → 2: 챕터 완성 기록 테이블. 기존 답변은 그대로 둔다. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `chapter_state` (`chapterNumber` INTEGER NOT NULL, `completedAt` INTEGER NOT NULL, PRIMARY KEY(`chapterNumber`))")
            }
        }

        /** 3 → 4: 시연 모드 설정. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `demoMode` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * 2 → 3: 여러 권 자서전과 설정. 기존 답변·챕터 기록은 1번 책으로 옮기고,
         * 그 책을 지금 쓰는 책으로 둔다(첫 실행 화면을 다시 띄우지 않는다).
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()
                db.execSQL("CREATE TABLE IF NOT EXISTS `book` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `ownerCallName` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `currentChapter` INTEGER NOT NULL, `publishRequestedAt` INTEGER)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `settings` (`id` INTEGER NOT NULL, `ownerName` TEXT NOT NULL, `callName` TEXT NOT NULL, `proactiveTalk` INTEGER NOT NULL, `chimeOn` INTEGER NOT NULL, `radioPublic` INTEGER NOT NULL, `activeBookId` INTEGER, PRIMARY KEY(`id`))")
                // 지금 챕터: 마무리한 챕터가 있으면 그다음, 없으면 2장(시연 기본값).
                db.execSQL("INSERT INTO `book` (`id`, `title`, `ownerCallName`, `createdAt`, `currentChapter`) VALUES (1, '김○○의 자서전', '○○님', $now, (SELECT COALESCE(MAX(`chapterNumber`) + 1, 2) FROM `chapter_state`))")
                db.execSQL("INSERT INTO `settings` VALUES (1, '김○○', '○○님', 1, 1, 0, 1)")

                db.execSQL("ALTER TABLE `answer` ADD COLUMN `bookId` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_answer_bookId` ON `answer` (`bookId`)")

                db.execSQL("CREATE TABLE IF NOT EXISTS `chapter_state_new` (`bookId` INTEGER NOT NULL, `chapterNumber` INTEGER NOT NULL, `completedAt` INTEGER NOT NULL, PRIMARY KEY(`bookId`, `chapterNumber`))")
                db.execSQL("INSERT INTO `chapter_state_new` SELECT 1, `chapterNumber`, `completedAt` FROM `chapter_state`")
                db.execSQL("DROP TABLE `chapter_state`")
                db.execSQL("ALTER TABLE `chapter_state_new` RENAME TO `chapter_state`")
            }
        }
    }
}
