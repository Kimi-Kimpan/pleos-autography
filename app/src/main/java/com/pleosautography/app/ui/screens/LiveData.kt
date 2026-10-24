package com.pleosautography.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.pleosautography.app.data.audio.RadioPlayback
import com.pleosautography.app.data.demo.ChapterStatus
import com.pleosautography.app.data.demo.ChapterSummary
import com.pleosautography.app.data.demo.DemoData
import com.pleosautography.app.data.repo.BookStats
import com.pleosautography.app.data.repo.ChapterProgress
import com.pleosautography.app.domain.interview.InterviewSession
import com.pleosautography.app.domain.interview.InterviewUi

/**
 * 책 전체 상태(저장소에서 읽는다).
 * @param current 지금 쓰는 챕터 번호.
 * @param done 마무리한 챕터 번호들.
 * @param stories 챕터별 이야기(답한 질문) 수.
 */
@Immutable
data class BookState(
    val current: Int = InterviewSession.FIRST_WRITING_CHAPTER,
    val done: Set<Int> = emptySet(),
    val stories: Map<Int, Int> = emptyMap(),
    val stats: BookStats? = null,
)

val LocalBookState = staticCompositionLocalOf { BookState() }

/** 저장소에서 읽은 지금 쓰는 챕터의 진행 상황. 아직 읽기 전이면 null(데모 값을 쓴다). */
val LocalLiveProgress = staticCompositionLocalOf<ChapterProgress?> { null }

/** 인터뷰 루프의 지금 단계(질문·음량·다 채움). */
val LocalInterviewUi = staticCompositionLocalOf { InterviewUi() }

/** 오토그래피 라디오 재생 상태(멈춤·소리 줄임). */
val LocalRadioPlayback = staticCompositionLocalOf { RadioPlayback() }

/** 저장소 값으로 챕터 상태·진행률·편 수를 바꾼다(저장소에 질문이 없는 챕터는 데모 값 그대로). */
@Composable
@ReadOnlyComposable
fun ChapterSummary.live(): ChapterSummary {
    val book = LocalBookState.current
    val progress = LocalLiveProgress.current?.takeIf { it.chapterNumber == number }
    val status = when {
        number in book.done -> ChapterStatus.DONE
        number == book.current -> ChapterStatus.WRITING
        status == ChapterStatus.WRITING -> ChapterStatus.NOT_STARTED
        else -> status
    }
    return copy(
        status = status,
        progress = when {
            status == ChapterStatus.DONE -> 1f
            progress != null -> progress.progress
            else -> this.progress
        },
        storyCount = progress?.answered ?: book.stories[number] ?: storyCount,
    )
}

/** 지금 쓰는 챕터. */
@Composable
@ReadOnlyComposable
fun currentChapter(): ChapterSummary {
    val current = LocalBookState.current.current
    return (DemoData.chapters.firstOrNull { it.number == current } ?: DemoData.chapters.first { it.status == ChapterStatus.WRITING }).live()
}
