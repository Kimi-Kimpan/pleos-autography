package com.pleosautography.app.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.pleosautography.app.data.audio.RadioPlayback
import com.pleosautography.app.data.audio.RadioPlayer
import com.pleosautography.app.data.db.AnswerEntity
import com.pleosautography.app.data.repo.AutographyRepository
import com.pleosautography.app.data.repo.ChapterProgress
import com.pleosautography.app.data.repo.Library
import com.pleosautography.app.data.speech.SpeechInput
import com.pleosautography.app.domain.interview.InterviewSession
import com.pleosautography.app.domain.listen.ListenSession
import com.pleosautography.app.domain.trigger.SituationController
import com.pleosautography.app.ui.screens.BookState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 화면이 쓰는 앱 서비스 묶음(AppGraph에서 만든다). 미리보기·테스트에서는 없이(null) 그린다.
 * @param library 설정과 책 목록(null이면 아직 읽는 중).
 * @param currentProgress 지금 쓰는 챕터의 진행 상황.
 * @param book 지금 쓰는 책의 상태(제목·부르는 이름·지금 챕터·마무리한 챕터·통계).
 * @param answersFor 지금 쓰는 책의 챕터 답변(원고 보기).
 * @param playChime 오토가 먼저 말을 걸기 전의 알림 멜로디(설정에서 끄면 울리지 않는다).
 * @param setTalking 인터뷰·원고 듣기 중 다른 소리를 멈춘다.
 * @param readAloud 원고 한 문장을 오토 목소리로 읽는다(시연은 읽는 시간만 기다린다).
 * @param createBook 첫 실행·새 자서전을 마치면 책을 만든다(제목, 이름, 부르는 이름, 첫 챕터).
 * @param launch 화면이 닫혀도 끝까지 해야 하는 저장 작업을 앱 범위에서 돌린다.
 * @param demoFixSentence 원고 듣기의 마이크를 누르면 말한 것으로 칠 준비된 정정 문장(시연용).
 */
class AppServices(
    val situation: SituationController,
    val interview: InterviewSession,
    val listen: ListenSession,
    val speechInput: SpeechInput,
    val radio: StateFlow<RadioPlayback>,
    val radioPlayer: RadioPlayer,
    val repository: AutographyRepository,
    val library: StateFlow<Library?>,
    val currentProgress: Flow<ChapterProgress?>,
    val book: Flow<BookState>,
    val answersFor: (Int) -> Flow<List<AnswerEntity>>,
    val playChime: suspend () -> Unit,
    val setTalking: (Boolean) -> Unit,
    val readAloud: suspend (String) -> Unit,
    val createBook: suspend (title: String, ownerName: String, callName: String, firstChapter: Int) -> Unit,
    val launch: (suspend () -> Unit) -> Unit,
    val demoFixSentence: String,
)

/** 화면 안쪽에서 앱 서비스를 쓸 때(없으면 null: 미리보기). */
val LocalAppServices = staticCompositionLocalOf<AppServices?> { null }
