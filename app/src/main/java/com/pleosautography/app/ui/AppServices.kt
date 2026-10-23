package com.pleosautography.app.ui

import com.pleosautography.app.data.audio.RadioPlayback
import com.pleosautography.app.data.repo.ChapterProgress
import com.pleosautography.app.data.speech.SpeechInput
import com.pleosautography.app.domain.interview.InterviewSession
import com.pleosautography.app.domain.trigger.SituationController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 화면이 쓰는 앱 서비스 묶음(AppGraph에서 만든다). 미리보기·테스트에서는 없이(null) 그린다.
 * @param playChime 오토가 먼저 말을 걸기 전의 알림 멜로디(다른 소리를 잠시 줄인다).
 * @param setTalking 인터뷰·원고 듣기 중 다른 소리를 멈춘다.
 */
class AppServices(
    val situation: SituationController,
    val interview: InterviewSession,
    val speechInput: SpeechInput,
    val radio: StateFlow<RadioPlayback>,
    val currentChapter: Flow<ChapterProgress>,
    val playChime: suspend () -> Unit,
    val setTalking: (Boolean) -> Unit,
)
