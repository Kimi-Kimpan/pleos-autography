package com.pleosautography.app.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.pleosautography.app.data.audio.RadioPlayback
import com.pleosautography.app.data.db.AnswerEntity
import com.pleosautography.app.data.repo.ChapterProgress
import com.pleosautography.app.data.speech.SpeechInput
import com.pleosautography.app.domain.interview.InterviewSession
import com.pleosautography.app.domain.trigger.SituationController
import com.pleosautography.app.ui.screens.BookState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 화면이 쓰는 앱 서비스 묶음(AppGraph에서 만든다). 미리보기·테스트에서는 없이(null) 그린다.
 * @param currentProgress 지금 쓰는 챕터의 진행 상황.
 * @param book 책 전체 상태(지금 챕터·마무리한 챕터·통계).
 * @param answersFor 챕터의 답변(원고 보기).
 * @param playChime 오토가 먼저 말을 걸기 전의 알림 멜로디(다른 소리를 잠시 줄인다).
 * @param setTalking 인터뷰·원고 듣기 중 다른 소리를 멈춘다.
 */
class AppServices(
    val situation: SituationController,
    val interview: InterviewSession,
    val speechInput: SpeechInput,
    val radio: StateFlow<RadioPlayback>,
    val currentProgress: Flow<ChapterProgress>,
    val book: Flow<BookState>,
    val answersFor: (Int) -> Flow<List<AnswerEntity>>,
    val playChime: suspend () -> Unit,
    val setTalking: (Boolean) -> Unit,
)

/** 화면 안쪽에서 앱 서비스를 쓸 때(없으면 null: 미리보기). */
val LocalAppServices = staticCompositionLocalOf<AppServices?> { null }
