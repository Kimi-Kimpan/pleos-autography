package com.pleosautography.app.data.demo

/** 챕터 상태(Room 초안의 Chapter.status와 같다). */
enum class ChapterStatus { NOT_STARTED, WRITING, DONE }

data class ChapterSummary(
    val number: Int,
    val title: String,
    val status: ChapterStatus,
    val storyCount: Int = 0,
    val progress: Float = 0f,
    val lastStory: String? = null,
)

data class NowPlaying(val label: String, val title: String, val meta: String)

/** 마일스톤 1~2용 데모 데이터. 마일스톤 4에서 Room 저장소로 바뀐다. */
object DemoData {
    const val OWNER_CALL_NAME = "○○"
    const val BOOK_TITLE = "김○○의 자서전"

    // 07~12장 제목은 질문 은행 파일에서 정한다(마일스톤 4).
    val chapters = listOf(
        ChapterSummary(1, "나의 뿌리", ChapterStatus.DONE, storyCount = 6, progress = 1f),
        ChapterSummary(2, "첫 차", ChapterStatus.WRITING, storyCount = 3, progress = 0.45f, lastStory = "처음 운전대를 잡은 날"),
        ChapterSummary(3, "일과 생계", ChapterStatus.NOT_STARTED),
        ChapterSummary(4, "가족", ChapterStatus.NOT_STARTED),
        ChapterSummary(5, "길 위에서 만난 사람들", ChapterStatus.NOT_STARTED),
        ChapterSummary(6, "가장 길었던 밤", ChapterStatus.NOT_STARTED),
    )

    val nowPlaying = NowPlaying(
        label = "오토그래피 라디오 · 재생 중",
        title = "짐칸 가득, 하루치의 삶",
        meta = "이○○ 기사님 · 1톤 화물차 18년",
    )
}
