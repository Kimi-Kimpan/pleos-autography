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
import com.pleosautography.app.data.demo.DemoData
import com.pleosautography.app.ui.components.HLine
import com.pleosautography.app.ui.components.ProgressBar
import com.pleosautography.app.ui.components.RuledText
import com.pleosautography.app.ui.components.StatusChip
import com.pleosautography.app.ui.components.StopHereButton
import com.pleosautography.app.ui.components.VoiceWave
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
 */
@Composable
fun InterviewScreen(onStopHere: () -> Unit, modifier: Modifier = Modifier) {
    val chapter = DemoData.chapters.first { it.status == ChapterStatus.WRITING }
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
                    StatusChip("주행 중 · 인터뷰", filled = true, sizePx = 22f)
                    BasicText("도착까지 ${demo.minutesToArrival}분", style = Arrival.style())
                }
                BasicText("${chapter.number}장 ${chapter.title} · 질문 ${demo.questionNumber}", style = Title.style(), maxLines = 1)
            }
            StopHereButton(onStopHere)
        }
        HLine(color = AutoColors.Ink)

        Column(
            modifier = Modifier.padding(start = 40.px, top = 40.px, end = 40.px),
            verticalArrangement = Arrangement.spacedBy(16.px),
        ) {
            BasicText("오토가 물었어요", style = AskedLabel.style(AutoColors.InkSecondary))
            RuledText(demo.question, Question, Modifier.fillMaxWidth())
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 40.px, top = 20.px, end = 40.px, bottom = 60.px),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.px, Alignment.CenterVertically),
        ) {
            VoiceWave()
            BasicText("듣고 있어요", style = Listening.style(AutoColors.InkSecondary))
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
            BasicText(
                text = demo.followUpHint,
                style = Hint.style(AutoColors.InkMuted).copy(textAlign = TextAlign.Center),
                modifier = Modifier.padding(top = 56.px),
            )
        }
    }
}
