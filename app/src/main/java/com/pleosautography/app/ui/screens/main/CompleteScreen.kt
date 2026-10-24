package com.pleosautography.app.ui.screens.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.pleosautography.app.data.demo.DemoData
import com.pleosautography.app.ui.components.PillButton
import com.pleosautography.app.ui.components.RuledText
import com.pleosautography.app.ui.components.ScreenStatusChip
import com.pleosautography.app.ui.icons.AutoIcons
import com.pleosautography.app.ui.screens.LocalBookState
import com.pleosautography.app.ui.theme.AutoColors
import com.pleosautography.app.ui.theme.GowunBatang
import com.pleosautography.app.ui.theme.TextSpec
import com.pleosautography.app.ui.theme.px

private val Headline = TextSpec(54f, FontWeight.Bold, GowunBatang, lineHeightPx = 76f)
private val StatNumber = TextSpec(48f, FontWeight.ExtraBold, tabularNumbers = true)
private val StatLabel = TextSpec(22f, FontWeight.SemiBold)
private val Caption = TextSpec(22f, FontWeight.SemiBold)

/** 자서전 전체 챕터 수. */
const val TOTAL_CHAPTERS = 12

/**
 * 10 자서전 완성. 시안: design/screens/Complete.png
 * 12개 챕터를 모두 채웠을 때만 연다. 한 챕터 완성은 인터뷰 화면 안에서 보여 준다(ChapterFull·ChapterDone).
 */
@Composable
fun CompleteScreen(onPublish: () -> Unit, onHome: () -> Unit, modifier: Modifier = Modifier) {
    val stats = LocalBookState.current.stats
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 40.px, top = 48.px, end = 40.px, bottom = 40.px),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(30.px),
    ) {
        ScreenStatusChip("정차 중 · P", filled = false, sizePx = 22f, modifier = Modifier.align(Alignment.Start))
        DoneRing()
        RuledText(
            text = "모든 챕터를 채우셨어요!\n이제 출판 준비를 해 볼까요?",
            spec = Headline,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(56.px)) {
            Stat(stats?.doneChapters ?: TOTAL_CHAPTERS, "챕터")
            Stat(stats?.stories ?: DemoData.book.storyCount, "이야기")
            Stat(stats?.conversations ?: DemoData.book.conversationCount, "오토와 나눈 대화")
            Stat(stats?.pages ?: 0, "원고 쪽수")
        }
        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(16.px)) {
            PillButton(
                text = "책 출판 신청",
                onClick = onPublish,
                modifier = Modifier.fillMaxWidth(),
                icon = AutoIcons.Book,
                filled = true,
                heightPx = 120f,
                iconSizePx = 30f,
                textSpec = TextSpec(36f, FontWeight.ExtraBold),
            )
            PillButton(
                text = "마치기 · 홈으로",
                onClick = onHome,
                modifier = Modifier.fillMaxWidth(),
                heightPx = 96f,
                textSpec = TextSpec(32f, FontWeight.ExtraBold),
            )
            BasicText(
                "출판을 신청하시면 담당자가 연락드려요 · ${stats?.doneChapters ?: TOTAL_CHAPTERS} / $TOTAL_CHAPTERS 챕터 완성",
                style = Caption.style(AutoColors.InkSecondary).copy(textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 점선 원(시안 SVG: viewBox 440, r 186, 두께 36, 점선 7·12.48) 안에 체크와 100%. */
@Composable
private fun DoneRing() {
    Box(Modifier.size(360.px), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(360.px)) {
            val unit = size.minDimension / 440f
            drawCircle(
                color = AutoColors.Ink,
                radius = 186f * unit,
                style = Stroke(width = 36f * unit, pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f * unit, 12.48f * unit))),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.px)) {
            Image(rememberVectorPainter(AutoIcons.Check), null, Modifier.size(84.px), colorFilter = ColorFilter.tint(AutoColors.Ink))
            BasicText("100%", style = TextSpec(40f, FontWeight.ExtraBold).style())
        }
    }
}

@Composable
private fun Stat(value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.px)) {
        BasicText("$value", style = StatNumber.style())
        BasicText(label, style = StatLabel.style(AutoColors.InkSecondary))
    }
}
