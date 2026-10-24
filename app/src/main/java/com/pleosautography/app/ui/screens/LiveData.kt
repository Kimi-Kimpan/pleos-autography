package com.pleosautography.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.pleosautography.app.data.audio.RadioPlayback
import com.pleosautography.app.data.audio.RadioState
import com.pleosautography.app.data.demo.ChapterStatus
import com.pleosautography.app.data.demo.ChapterSummary
import com.pleosautography.app.data.demo.DemoData
import com.pleosautography.app.data.repo.BookStats
import com.pleosautography.app.data.repo.ChapterProgress
import com.pleosautography.app.domain.interview.InterviewSession
import com.pleosautography.app.domain.interview.InterviewUi
import com.pleosautography.app.domain.listen.ListenSession

/**
 * 지금 쓰는 책의 상태(저장소에서 읽는다).
 * @param id 책 id. 책이 없으면(첫 실행 전) null.
 * @param callName 오토가 부르는 이름(“○○님”).
 * @param current 지금 쓰는 챕터 번호.
 * @param done 마무리한 챕터 번호들.
 * @param stories 챕터별 이야기(답한 질문) 수.
 */
@Immutable
data class BookState(
    val id: Long? = null,
    val title: String = NEW_BOOK_TITLE,
    val callName: String = "",
    val current: Int = InterviewSession.FIRST_WRITING_CHAPTER,
    val done: Set<Int> = emptySet(),
    val stories: Map<Int, Int> = emptyMap(),
    val stats: BookStats? = null,
    val publishRequested: Boolean = false,
) {
    /** 인사말에 쓰는 이름. 아직 정하지 않았으면 빈칸 없이 “안녕하세요!”가 되게 한다. */
    val greetingName: String get() = if (callName.isBlank()) "" else " $callName"

    companion object {
        const val NEW_BOOK_TITLE = "새 자서전"
    }
}

val LocalBookState = staticCompositionLocalOf { BookState() }

/** 저장소에서 읽은 지금 쓰는 챕터의 진행 상황. 아직 읽기 전이면 null(데모 값을 쓴다). */
val LocalLiveProgress = staticCompositionLocalOf<ChapterProgress?> { null }

/** 인터뷰 루프의 지금 단계(질문·음량·다 채움). */
val LocalInterviewUi = staticCompositionLocalOf { InterviewUi() }

/** 오토그래피 라디오 소리 상태(인터뷰 중 멈춤·소리 줄임). */
val LocalRadioPlayback = staticCompositionLocalOf { RadioPlayback() }

/** 오토그래피 라디오 재생 위치(지금 편·다음 편·일시정지). */
val LocalRadioState = staticCompositionLocalOf { RadioState() }

/** 저장소 값으로 챕터 상태·진행률·편 수를 정한다. */
@Composable
@ReadOnlyComposable
fun ChapterSummary.live(): ChapterSummary {
    val book = LocalBookState.current
    val progress = LocalLiveProgress.current?.takeIf { it.chapterNumber == number && number == book.current }
    val status = when {
        number in book.done -> ChapterStatus.DONE
        number == book.current -> ChapterStatus.WRITING
        else -> ChapterStatus.NOT_STARTED
    }
    return copy(
        status = status,
        progress = when {
            status == ChapterStatus.DONE -> 1f
            progress != null -> progress.progress
            else -> 0f
        },
        storyCount = progress?.answered ?: book.stories[number] ?: 0,
        lastStory = progress?.lastAnswer?.transcript?.let { ListenSession.splitSentences(it).firstOrNull() },
    )
}

/** 챕터 번호의 요약(제목은 DemoData, 07~12장은 “N장”). */
@Composable
@ReadOnlyComposable
fun chapterSummary(number: Int): ChapterSummary =
    (DemoData.chapters.firstOrNull { it.number == number } ?: ChapterSummary(number, InterviewSession.titleOf(number), ChapterStatus.NOT_STARTED)).live()

/** 지금 쓰는 챕터. */
@Composable
@ReadOnlyComposable
fun currentChapter(): ChapterSummary = chapterSummary(LocalBookState.current.current)
