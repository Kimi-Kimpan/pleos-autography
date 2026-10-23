package com.pleosautography.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.pleosautography.app.data.audio.RadioPlayback
import com.pleosautography.app.data.demo.ChapterSummary
import com.pleosautography.app.data.repo.ChapterProgress
import com.pleosautography.app.domain.interview.InterviewUi

/** 저장소에서 읽은 지금 쓰는 챕터의 진행 상황. 아직 읽기 전이면 null(데모 값을 쓴다). */
val LocalLiveProgress = staticCompositionLocalOf<ChapterProgress?> { null }

/** 인터뷰 루프의 지금 단계(질문·음량·받아 적은 문장). */
val LocalInterviewUi = staticCompositionLocalOf { InterviewUi() }

/** 오토그래피 라디오 재생 상태(멈춤·소리 줄임). */
val LocalRadioPlayback = staticCompositionLocalOf { RadioPlayback() }

/** 이 챕터가 저장소에 있으면 진행률·편 수를 실제 값으로 바꾼다. */
@Composable
@ReadOnlyComposable
fun ChapterSummary.live(): ChapterSummary {
    val live = LocalLiveProgress.current ?: return this
    if (live.chapterNumber != number) return this
    return copy(progress = live.progress, storyCount = live.answered)
}
