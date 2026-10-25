package com.pleosautography.app.ui.screens.drive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.pleosautography.app.data.demo.ChapterSummary
import com.pleosautography.app.data.demo.DemoData
import com.pleosautography.app.domain.DriveMode
import com.pleosautography.app.domain.interview.InterviewPhase
import com.pleosautography.app.domain.interview.InterviewSession
import com.pleosautography.app.ui.components.HLine
import com.pleosautography.app.ui.components.PillButton
import com.pleosautography.app.ui.components.ProgressBar
import com.pleosautography.app.ui.components.RuledText
import com.pleosautography.app.ui.components.ScreenStatusChip
import com.pleosautography.app.ui.components.StopHereButton
import com.pleosautography.app.ui.components.VoiceWave
import com.pleosautography.app.ui.icons.AutoIcons
import com.pleosautography.app.ui.screens.LocalInterviewUi
import com.pleosautography.app.ui.screens.LocalLiveProgress
import com.pleosautography.app.ui.screens.currentChapter
import com.pleosautography.app.ui.screens.live
import com.pleosautography.app.ui.theme.AutoColors
import com.pleosautography.app.ui.theme.GowunBatang
import com.pleosautography.app.ui.theme.LocalDriveMode
import com.pleosautography.app.ui.theme.TextSpec
import com.pleosautography.app.ui.theme.px
import kotlin.math.roundToInt

private val Arrival = TextSpec(22f, FontWeight.Bold)
private val Title = TextSpec(40f, FontWeight.ExtraBold)
private val AskedLabel = TextSpec(24f, FontWeight.Bold)
private val Question = TextSpec(58f, FontWeight.Bold, GowunBatang, lineHeightPx = 82f, letterSpacingEm = -0.015f)
private val FullQuestion = TextSpec(54f, FontWeight.Bold, GowunBatang, lineHeightPx = 80f, letterSpacingEm = -0.015f)
private val Listening = TextSpec(44f, FontWeight.ExtraBold)
private val ListeningSmall = TextSpec(32f, FontWeight.ExtraBold)
private val Percent = TextSpec(60f, FontWeight.ExtraBold, tabularNumbers = true)
private val Filling = TextSpec(28f, FontWeight.Bold)
private val Hint = TextSpec(36f, FontWeight.Bold, GowunBatang, lineHeightPx = 54f)
private val FinishText = TextSpec(36f, FontWeight.ExtraBold)
private val ChapterButtonText = TextSpec(29f, FontWeight.ExtraBold)
private val Caption = TextSpec(22f, FontWeight.SemiBold)

/** 챕터를 다 채운 화면의 버튼. */
class ChapterFullActions(
    val onFinish: () -> Unit,
    val onNextChapter: () -> Unit,
    val onReadScript: () -> Unit,
)

/**
 * 04 음성 인터뷰. 시안: design/screens/Interview.png
 * 이미 듣는 중이라 마이크 대신 음성 파형을 보여 준다. 오토의 말(질문·후속 질문)은 명조·노트 줄로 쓴다.
 * 챕터 질문을 모두 채우면 같은 화면에서 안내 문구와 버튼만 바뀐다(시안 ChapterFull, 정차 중 ChapterDone).
 *
 * @param parkedPane 가로 전체 창 정차 중 오른쪽 칸에 열 때(시안 FullParkInterview).
 *   머리글 없이 질문부터 시작한다. ‘여기까지’는 왼쪽 목록의 그 챕터 ‘이어쓰기’ 자리에 파랗게 둔다(이 칸에는 두지 않는다).
 */
@Composable
fun InterviewScreen(
    onStopHere: () -> Unit,
    fullActions: ChapterFullActions,
    modifier: Modifier = Modifier,
    parkedPane: Boolean = false,
) {
    val ui = LocalInterviewUi.current
    // 인터뷰가 다루는 챕터(마무리한 뒤에도 그 챕터를 보인다). 아직 시작 전이면 지금 쓰는 챕터.
    val chapter = DemoData.chapters.firstOrNull { it.number == ui.chapter }?.live() ?: currentChapter()
    val full = ui.chapterFull
    Column(modifier.fillMaxSize()) {
        if (!parkedPane) {
            Header(chapter, full, onStopHere)
            HLine(color = AutoColors.Ink)
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(if (parkedPane) Modifier.padding(start = 40.px, top = 36.px, end = 40.px, bottom = 44.px) else Modifier),
        ) {
            QuestionBlock(
                full = full,
                modifier = if (parkedPane) {
                    Modifier.padding(start = 16.px, top = 76.px, end = 16.px)
                } else {
                    Modifier.padding(start = 40.px, top = 40.px, end = 40.px)
                },
            )
            if (full) {
                ChapterFullBlock(chapter, fullActions, Modifier.weight(1f).padding(horizontal = if (parkedPane) 0.px else 40.px))
            } else {
                ListeningBlock(
                    chapter = chapter,
                    hintTopPx = if (parkedPane) 40f else 56f,
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            start = if (parkedPane) 16.px else 40.px,
                            top = if (parkedPane) 10.px else 20.px,
                            end = if (parkedPane) 16.px else 40.px,
                            bottom = if (parkedPane) 10.px else 60.px,
                        ),
                )
            }
        }
    }
}

@Composable
private fun Header(chapter: ChapterSummary, full: Boolean, onStopHere: () -> Unit) {
    val live = LocalLiveProgress.current?.takeIf { it.chapterNumber == chapter.number }
    val number = LocalInterviewUi.current.question?.order ?: live?.questionNumber ?: DemoData.drive.questionNumber
    // 다 채운 뒤에는 “질문 7 / 7”처럼 전체 수와 함께 쓴다(시안 ChapterFull). 마무리한 챕터는 이야기 수로.
    val total = live?.total ?: chapter.storyCount
    val questionText = if (full) "질문 $total / $total" else "질문 $number"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 40.px, top = 34.px, end = 40.px, bottom = 30.px),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.px),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.px)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.px)) {
                val driving = LocalDriveMode.current == DriveMode.DRIVING
                ScreenStatusChip(if (driving) "주행 중 · 인터뷰" else "정차 중 · P", filled = driving, sizePx = 22f)
                if (driving) {
                    LocalDriveSituation.current.minutesToArrival?.let { BasicText("도착까지 ${it}분", style = Arrival.style()) }
                }
            }
            BasicText("${chapter.number}장 ${chapter.title} · $questionText", style = Title.style(), maxLines = 1)
        }
        StopHereButton(onStopHere)
    }
}

@Composable
private fun QuestionBlock(full: Boolean, modifier: Modifier) {
    val ui = LocalInterviewUi.current
    val text = ui.prompt
        ?: if (full) InterviewSession.fullPrompt(ui.chapter.takeIf { it > 0 } ?: currentChapter().number) else null
        ?: ui.question?.text
        ?: LocalLiveProgress.current?.nextQuestion?.text
        ?: DemoData.drive.question
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.px)) {
        BasicText("오토가 물었어요", style = AskedLabel.style(AutoColors.InkSecondary))
        // 세 줄 이상인 말(다 채움 안내·긴 후속 질문)은 시안 ChapterFull처럼 한 단계 작게 쓴다.
        val long = full || text.count { it == '\n' } >= 2
        RuledText(text, if (long) FullQuestion else Question, Modifier.fillMaxWidth())
    }
}

@Composable
private fun ListeningBlock(chapter: ChapterSummary, hintTopPx: Float, modifier: Modifier) {
    val ui = LocalInterviewUi.current
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(22.px, Alignment.CenterVertically),
    ) {
        VoiceWave(levels = waveLevels(ui.phase, ui.level))
        BasicText(ui.phase.label(), style = Listening.style(AutoColors.InkSecondary))
        ProgressBar(
            progress = chapter.progress,
            heightPx = 16f,
            modifier = Modifier
                .padding(top = 36.px)
                .widthIn(max = 580.px),
        )
        // 시안의 줄 높이(1em)가 글자 높이보다 작아서 상자 높이를 고정한다.
        Box(Modifier.height(60.px), contentAlignment = Alignment.Center) {
            BasicText("${(chapter.progress * 100).roundToInt()}%", style = Percent.style())
        }
        BasicText("‘${chapter.title}’가 채워지고 있어요", style = Filling.style())
        // 후속 질문을 하는 중이면 그 질문이 힌트 역할을 하므로 힌트는 숨긴다.
        val followingUp = ui.prompt != null && ui.question != null && ui.prompt != ui.question.text
        if (!followingUp) BasicText(
            text = ui.question?.followUpHint ?: LocalLiveProgress.current?.nextQuestion?.followUpHint ?: DemoData.drive.followUpHint,
            style = Hint.style(AutoColors.InkMuted).copy(textAlign = TextAlign.Center),
            modifier = Modifier.padding(top = hintTopPx.px),
        )
    }
}

/** 다 채운 뒤: 작은 파형·“듣고 있어요”, 진행률은 숨기고 아래에 버튼. 계속 듣고 있어 말을 이어 가면 저장한다. */
@Composable
private fun ChapterFullBlock(chapter: ChapterSummary, actions: ChapterFullActions, modifier: Modifier) {
    val ui = LocalInterviewUi.current
    val driving = LocalDriveMode.current == DriveMode.DRIVING
    val next = DemoData.chapters.firstOrNull { it.number == chapter.number + 1 }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 44.px, bottom = 44.px),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.px),
    ) {
        VoiceWave(levels = waveLevels(ui.phase, ui.level), barWidthPx = 10f, gapPx = 9f, heightPx = 110f)
        BasicText("듣고 있어요", style = ListeningSmall.style(AutoColors.InkSecondary))
        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(16.px)) {
            PillButton(
                text = "챕터 마무리",
                onClick = actions.onFinish,
                modifier = Modifier.fillMaxWidth(),
                icon = AutoIcons.Check,
                filled = true,
                heightPx = 120f,
                iconSizePx = 30f,
                textSpec = FinishText,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.px)) {
                PillButton(
                    text = next?.let { "다음 챕터 · ${it.title}" } ?: "다음 챕터",
                    onClick = actions.onNextChapter,
                    modifier = Modifier.weight(1f),
                    icon = AutoIcons.Pencil,
                    heightPx = 96f,
                    horizontalPaddingPx = 0f,
                    textSpec = ChapterButtonText,
                )
                // 원고 읽기는 정차 중에만 연다.
                PillButton(
                    text = "원고 읽어 보기",
                    onClick = actions.onReadScript,
                    modifier = Modifier.weight(1f),
                    icon = AutoIcons.Document,
                    enabled = !driving,
                    heightPx = 96f,
                    horizontalPaddingPx = 0f,
                    textSpec = ChapterButtonText,
                )
            }
            BasicText(
                if (driving) "원고 읽어 보기는 정차 중에 열려요" else "정차 중에는 원고를 바로 읽어 볼 수 있어요",
                style = Caption.style(AutoColors.InkMuted).copy(textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 듣는 동안에는 실제 마이크 음량으로 파형이 움직인다. 오토가 말하는 동안에는 낮게 둔다. */
private fun waveLevels(phase: InterviewPhase, level: Float): List<Float> {
    val scale = when (phase) {
        // 조용할 때도 쉬는 모양을 보이고, 목소리가 들리면 커진다.
        InterviewPhase.LISTENING -> 0.45f + 0.55f * level
        InterviewPhase.IDLE -> 1f
        InterviewPhase.ASKING -> 0.2f
    }
    return RestingShape.map { it * scale }
}

/** 시안의 막대 높이 비율(44~186px / 190). */
private val RestingShape = listOf(44f, 92f, 150f, 96f, 186f, 140f, 170f, 86f, 120f, 48f).map { it / 190f }

private fun InterviewPhase.label(): String = when (this) {
    InterviewPhase.ASKING -> "오토가 묻고 있어요"
    else -> "듣고 있어요"
}
