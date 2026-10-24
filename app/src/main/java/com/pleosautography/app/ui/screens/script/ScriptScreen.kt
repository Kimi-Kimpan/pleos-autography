package com.pleosautography.app.ui.screens.script

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.pleosautography.app.data.demo.DemoData
import com.pleosautography.app.data.demo.ManuscriptParagraph
import com.pleosautography.app.ui.LocalAppServices
import com.pleosautography.app.ui.components.HLine
import com.pleosautography.app.ui.components.PillButton
import com.pleosautography.app.ui.components.RuledText
import com.pleosautography.app.ui.components.ScreenStatusChip
import com.pleosautography.app.ui.icons.AutoIcons
import com.pleosautography.app.ui.screens.LocalInterviewUi
import com.pleosautography.app.ui.screens.currentChapter
import com.pleosautography.app.ui.screens.live
import com.pleosautography.app.ui.theme.AutoColors
import com.pleosautography.app.ui.theme.GowunBatang
import com.pleosautography.app.ui.theme.TextSpec
import com.pleosautography.app.ui.theme.px

private val Title = TextSpec(52f, FontWeight.ExtraBold, lineHeightPx = 57.2f)
private val Hint = TextSpec(24f, FontWeight.SemiBold)
private val Body = TextSpec(34f, FontWeight.Normal, GowunBatang, lineHeightPx = 58f)
private val ContinueAsk = TextSpec(36f, FontWeight.Bold, GowunBatang, lineHeightPx = 46.8f, letterSpacingEm = -0.01f)
private val Floating = TextSpec(27f, FontWeight.ExtraBold)
private val Caption = TextSpec(20f, FontWeight.SemiBold)


/**
 * 05 원고 보기(정차 중). 시안: design/screens/Script.png
 * 노트 줄 위 원고. 문단을 길게 누르면 그 부분을 고르고, 위에 말로 고치기·다시 듣기·지우기 알약 버튼을 띄운다.
 * 저장한 답변이 있으면 그 답변을, 없으면 예시 원고를 보인다. 말로 고치는 동작은 마일스톤 7에서 붙인다.
 * @param showHome 가로 전체 창에서는 왼쪽에 홈이 늘 있으므로 ‘홈’ 버튼을 뺀다.
 */
@Composable
fun ScriptScreen(
    onHome: () -> Unit,
    onContinueWriting: () -> Unit,
    onListenFromStart: () -> Unit,
    modifier: Modifier = Modifier,
    showHome: Boolean = true,
) {
    // 인터뷰에서 열었으면 그 챕터, 아니면 지금 쓰는 챕터.
    val interviewChapter = LocalInterviewUi.current.chapter
    val chapter = DemoData.chapters.firstOrNull { it.number == interviewChapter }?.live() ?: currentChapter()
    val services = LocalAppServices.current
    val answers = remember(services, chapter.number) { services?.answersFor?.invoke(chapter.number) }
        ?.collectAsState(initial = emptyList())?.value.orEmpty()
    val example = answers.isEmpty()
    val paragraphs = if (example) DemoData.manuscript else answers.map { ManuscriptParagraph(it.transcript) }
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }

    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.px, top = 32.px, end = 32.px, bottom = 28.px),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.px),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.px)) {
                ScreenStatusChip("정차 중 · P", filled = false)
                BasicText("${chapter.number}장 ${chapter.title} · 원고", style = Title.style(), maxLines = 1)
            }
            if (showHome) PillButton(
                text = "홈",
                onClick = onHome,
                icon = AutoIcons.ChevronLeft,
                heightPx = 100f,
                iconSizePx = 32f,
                startPaddingPx = 26f,
                endPaddingPx = 36f,
                gapPx = 10f,
                textSpec = TextSpec(34f, FontWeight.ExtraBold),
            )
        }
        HLine(color = AutoColors.Ink)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 32.px, top = 8.px, end = 32.px),
        ) {
            Row(
                modifier = Modifier.padding(top = 16.px, bottom = 6.px),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.px),
            ) {
                Image(rememberVectorPainter(AutoIcons.Select), null, Modifier.size(26.px), colorFilter = ColorFilter.tint(AutoColors.InkSecondary))
                BasicText("고칠 부분을 길게 눌러 고르면, 말로 고칠 수 있어요", style = Hint.style(AutoColors.InkSecondary))
            }
            paragraphs.forEachIndexed { index, paragraph ->
                Paragraph(
                    paragraph = paragraph,
                    selected = selected == index,
                    onLongPress = { selected = if (selected == index) null else index },
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .dashedBottomBorder()
                    .padding(vertical = 28.px),
                verticalArrangement = Arrangement.spacedBy(20.px),
            ) {
                BasicText("여기서부터 이어서 써 볼까요?", style = ContinueAsk.style())
                Row(horizontalArrangement = Arrangement.spacedBy(16.px)) {
                    PillButton("이어쓰기", onContinueWriting, icon = AutoIcons.Pencil, filled = true)
                    PillButton("처음부터 듣기", onListenFromStart, icon = AutoIcons.Play, iconSizePx = 26f)
                }
            }
            if (example) {
                BasicText("예시 원고", style = Caption.style(AutoColors.InkSecondary), modifier = Modifier.padding(top = 14.px))
            }
            Spacer(Modifier.height(32.px))
        }
    }
}

/** 원고 한 문단. 고르면 강조 구절(없으면 문단 전체)을 파랗게 칠하고 위에 알약 버튼을 띄운다. */
@Composable
private fun Paragraph(paragraph: ManuscriptParagraph, selected: Boolean, onLongPress: () -> Unit) {
    val text = buildAnnotatedString {
        val target = if (selected) paragraph.highlight ?: paragraph.text else null
        val start = target?.let { paragraph.text.indexOf(it) } ?: -1
        if (target == null || start < 0) {
            append(paragraph.text)
        } else {
            append(paragraph.text.substring(0, start))
            withStyle(SpanStyle(background = AutoColors.Ink, color = AutoColors.OnInk)) { append(target) }
            append(paragraph.text.substring(start + target.length))
        }
    }
    Box(Modifier.pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPress() }) }) {
        RuledText(text, Body, Modifier.fillMaxWidth())
        if (selected) {
            Row(
                modifier = Modifier
                    .wrapContentSize()
                    .offset(x = 250.px, y = (-86).px),
                horizontalArrangement = Arrangement.spacedBy(12.px),
            ) {
                FloatingPill("말로 고치기", filled = true)
                FloatingPill("다시 듣기", filled = false)
                FloatingPill("지우기", filled = false)
            }
        }
    }
}

/** 원고 위에 떠 있는 알약 버튼(높이 76). 동작은 마일스톤 7에서 붙인다. */
@Composable
private fun FloatingPill(text: String, filled: Boolean) {
    val shape = RoundedCornerShape(percent = 50)
    PillButton(
        text = text,
        onClick = {},
        modifier = Modifier
            .shadow(if (filled) 8.px else 6.px, shape, ambientColor = ShadowInk, spotColor = ShadowInk)
            .background(AutoColors.SurfacePill, shape),
        icon = if (filled) AutoIcons.MicFilled else null,
        filled = filled,
        heightPx = 76f,
        iconSizePx = 26f,
        horizontalPaddingPx = 26f,
        textSpec = Floating,
    )
}

private val ShadowInk = Color(0x291A0BD0)

/** 시안의 2.5px 점선 아래 선. */
@Composable
private fun Modifier.dashedBottomBorder(): Modifier {
    val width = 2.5.px
    val dash = 8.px
    return drawBehind {
        val w = width.toPx()
        drawLine(
            color = AutoColors.Ink,
            start = Offset(0f, size.height - w / 2),
            end = Offset(size.width, size.height - w / 2),
            strokeWidth = w,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), dash.toPx() * 0.75f)),
        )
    }
}
