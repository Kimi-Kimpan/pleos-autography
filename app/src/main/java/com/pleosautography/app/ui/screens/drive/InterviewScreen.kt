package com.pleosautography.app.ui.screens.drive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.pleosautography.app.data.demo.ChapterStatus
import com.pleosautography.app.data.demo.ChapterSummary
import com.pleosautography.app.data.demo.DemoData
import com.pleosautography.app.domain.interview.InterviewPhase
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
import com.pleosautography.app.ui.screens.live
import com.pleosautography.app.ui.theme.AutoColors
import com.pleosautography.app.ui.theme.GowunBatang
import com.pleosautography.app.ui.theme.TextSpec
import com.pleosautography.app.ui.theme.px
import kotlin.math.roundToInt

private val Arrival = TextSpec(22f, FontWeight.Bold)
private val Title = TextSpec(40f, FontWeight.ExtraBold)
private val AskedLabel = TextSpec(24f, FontWeight.Bold)
private val Question = TextSpec(58f, FontWeight.Bold, GowunBatang, lineHeightPx = 82f, letterSpacingEm = -0.015f)
private val Listening = TextSpec(44f, FontWeight.ExtraBold)
private val Percent = TextSpec(60f, FontWeight.ExtraBold, tabularNumbers = true)
private val Filling = TextSpec(28f, FontWeight.Bold)
private val Hint = TextSpec(36f, FontWeight.Bold, GowunBatang, lineHeightPx = 54f)

/**
 * 04 음성 인터뷰. 시안: design/screens/Interview.png
 * 이미 듣는 중이라 마이크 대신 음성 파형을 보여 준다.
 *
 * @param parkedPane 가로 전체 창 정차 중 오른쪽 칸에 열 때(시안 FullParkInterview).
 *   머리글 없이 질문부터 시작하고, ‘여기까지’는 아래 가운데에 둔다.
 */
@Composable
fun InterviewScreen(onStopHere: () -> Unit, modifier: Modifier = Modifier, parkedPane: Boolean = false) {
    val chapter = DemoData.chapters.first { it.status == ChapterStatus.WRITING }.live()
    if (parkedPane) {
        ParkedPaneInterview(chapter, onStopHere, modifier)
    } else {
        FullInterview(chapter, onStopHere, modifier)
    }
}

@Composable
private fun FullInterview(chapter: ChapterSummary, onStopHere: () -> Unit, modifier: Modifier) {
    val demo = DemoData.drive
    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 40.px, top = 34.px, end = 40.px, bottom = 30.px),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.px),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.px)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.px)) {
                    ScreenStatusChip("주행 중 · 인터뷰", filled = true, sizePx = 22f)
                    LocalDriveSituation.current.minutesToArrival?.let { BasicText("도착까지 ${it}분", style = Arrival.style()) }
                }
                val number = LocalInterviewUi.current.question?.order ?: LocalLiveProgress.current?.questionNumber ?: demo.questionNumber
                BasicText("${chapter.number}장 ${chapter.title} · 질문 $number", style = Title.style(), maxLines = 1)
            }
            StopHereButton(onStopHere)
        }
        HLine(color = AutoColors.Ink)

        QuestionBlock(Modifier.padding(start = 40.px, top = 40.px, end = 40.px))
        ListeningBlock(
            chapter = chapter,
            hintTopPx = 56f,
            modifier = Modifier
                .weight(1f)
                .padding(start = 40.px, top = 20.px, end = 40.px, bottom = 60.px),
        )
    }
}

@Composable
private fun ParkedPaneInterview(chapter: ChapterSummary, onStopHere: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 40.px, top = 36.px, end = 40.px, bottom = 44.px),
    ) {
        QuestionBlock(Modifier.padding(start = 16.px, top = 76.px, end = 16.px))
        ListeningBlock(
            chapter = chapter,
            hintTopPx = 40f,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.px, vertical = 10.px),
        )
        PillButton(
            text = "여기까지",
            onClick = onStopHere,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            icon = AutoIcons.Clip,
            heightPx = 96f,
            iconSizePx = 34f,
            startPaddingPx = 30f,
            endPaddingPx = 40f,
            textSpec = TextSpec(32f, FontWeight.ExtraBold),
        )
    }
}

@Composable
private fun QuestionBlock(modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.px)) {
        BasicText("오토가 물었어요", style = AskedLabel.style(AutoColors.InkSecondary))
        val question = LocalInterviewUi.current.question ?: LocalLiveProgress.current?.nextQuestion
        RuledText(question?.text ?: DemoData.drive.question, Question, Modifier.fillMaxWidth())
    }
}

@Composable
private fun ListeningBlock(chapter: ChapterSummary, hintTopPx: Float, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(22.px, Alignment.CenterVertically),
    ) {
        val ui = LocalInterviewUi.current
        // 듣는 동안에는 실제 마이크 음량으로 파형이 움직인다. 오토가 말하는 동안에는 낮게 둔다.
        val level = when (ui.phase) {
            InterviewPhase.LISTENING -> 0.2f + 0.8f * ui.level
            InterviewPhase.IDLE -> 1f
            else -> 0.2f
        }
        VoiceWave(levels = RestingShape.map { it * level })
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
        // 받아 적은 직후에는 그 문장을, 그 밖에는 다음 이야기 힌트를 보인다.
        val saved = ui.lastTranscript.takeIf { ui.phase == InterviewPhase.SAVED }
        BasicText(
            text = saved?.let { "“$it”" } ?: ui.question?.followUpHint ?: LocalLiveProgress.current?.nextQuestion?.followUpHint ?: DemoData.drive.followUpHint,
            style = Hint.style(if (saved != null) AutoColors.Ink else AutoColors.InkMuted).copy(textAlign = TextAlign.Center),
            modifier = Modifier.padding(top = hintTopPx.px),
        )
    }
}

/** 시안의 막대 높이 비율(44~186px / 190). */
private val RestingShape = listOf(44f, 92f, 150f, 96f, 186f, 140f, 170f, 86f, 120f, 48f).map { it / 190f }

private fun InterviewPhase.label(): String = when (this) {
    InterviewPhase.ASKING -> "오토가 묻고 있어요"
    InterviewPhase.SAVED -> "받아 적었어요"
    InterviewPhase.DONE -> "챕터를 다 채웠어요"
    else -> "듣고 있어요"
}
