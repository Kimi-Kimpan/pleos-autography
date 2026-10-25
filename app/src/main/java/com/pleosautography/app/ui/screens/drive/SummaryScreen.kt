package com.pleosautography.app.ui.screens.drive

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import com.pleosautography.app.domain.DriveMode
import com.pleosautography.app.domain.interview.InterviewSession
import com.pleosautography.app.domain.interview.InterviewSummary
import com.pleosautography.app.ui.LocalAppServices
import com.pleosautography.app.ui.components.HLine
import com.pleosautography.app.ui.components.PillButton
import com.pleosautography.app.ui.components.RingTimer
import com.pleosautography.app.ui.components.RuledText
import com.pleosautography.app.ui.components.ScreenStatusChip
import com.pleosautography.app.ui.icons.AutoIcons
import com.pleosautography.app.ui.theme.AutoColors
import com.pleosautography.app.ui.theme.GowunBatang
import com.pleosautography.app.ui.theme.LocalDemoOptions
import com.pleosautography.app.ui.theme.LocalDriveMode
import com.pleosautography.app.ui.theme.TextSpec
import com.pleosautography.app.ui.theme.px
import kotlin.math.roundToInt

private val Headline = TextSpec(54f, FontWeight.Bold, GowunBatang, lineHeightPx = 78f, letterSpacingEm = -0.015f)
private val Before = TextSpec(52f, FontWeight.Bold, tabularNumbers = true)
private val After = TextSpec(104f, FontWeight.ExtraBold, tabularNumbers = true, lineHeightPx = 110f)
private val Arrow = TextSpec(48f, FontWeight.Bold)
private val ChapterLine = TextSpec(26f, FontWeight.Bold)
private val StatName = TextSpec(28f, FontWeight.Bold)
private val StatValue = TextSpec(32f, FontWeight.ExtraBold, tabularNumbers = true)
private val Remaining = TextSpec(32f, FontWeight.Bold)
private val HomeText = TextSpec(38f, FontWeight.ExtraBold)
private val SimilarText = TextSpec(32f, FontWeight.ExtraBold)

/** 오토가 원고로 옮기는 동안 보여 주는 시간. */
const val SUMMARY_LOADING_MS = 2_000L

/** 주행 중 요약 화면은 이만큼 뒤 주행 홈으로 돌아간다. */
const val SUMMARY_DRIVE_TIMEOUT_SECONDS = 6

/** 링 타이머 바탕(파란 버튼 위). */
private val RingTrackOnInk = Color(0x66FFFFFF)

/**
 * 10-3 인터뷰 요약. ‘여기까지’를 누르거나 말하면(챕터를 다 채운 뒤 포함) 연다.
 * 먼저 약 2초 “오토가 오늘 이야기를 원고로 옮기고 있어요”(노트 줄이 채워진다)를 보이고, 이번 인터뷰의 요약을 보인다.
 * 주행 중에는 큰 글씨·버튼 2개만 두고, [SUMMARY_DRIVE_TIMEOUT_SECONDS]초 뒤 주행 홈으로 돌아간다.
 * @param onSimilar 나와 비슷한 이야기 듣기(정차 중 플레이어, 주행 중 라디오 재생).
 */
@Composable
fun SummaryScreen(onHome: () -> Unit, onSimilar: () -> Unit, modifier: Modifier = Modifier) {
    val summary = LocalAppServices.current?.interview?.summary?.collectAsState()?.value ?: DEMO_SUMMARY
    val timers = LocalDemoOptions.current.timersRunning
    var loading by rememberSaveable { mutableStateOf(true) }
    // 시연 스크린샷(timers false)에서는 정리 중 화면에 머문다.
    LaunchedEffect(timers) {
        if (loading && timers) {
            kotlinx.coroutines.delay(SUMMARY_LOADING_MS)
            loading = false
        }
    }
    if (loading) {
        LoadingContent(running = timers, modifier = modifier)
    } else {
        SummaryContent(summary, onHome, onSimilar, modifier)
    }
}

/** 정리 중: 명조 안내 + 노트 줄 위로 글줄이 차례로 채워진다. */
@Composable
private fun LoadingContent(running: Boolean, modifier: Modifier) {
    val progress = remember { Animatable(if (running) 0f else 0.6f) }
    LaunchedEffect(running) {
        if (running) progress.animateTo(1f, tween(SUMMARY_LOADING_MS.toInt() - 200, easing = LinearEasing))
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 40.px, top = 36.px, end = 40.px, bottom = 44.px),
    ) {
        StatusChipFor()
        RuledText(
            "오토가 오늘 이야기를\n원고로 옮기고 있어요",
            Headline,
            Modifier.padding(top = 150.px).fillMaxWidth(),
        )
        val lineGap = 84.px
        val ink = AutoColors.Ink
        val rule = AutoColors.Ruled
        Canvas(
            Modifier
                .padding(top = 70.px)
                .fillMaxWidth()
                .height(lineGap * WRITING_LINES.size),
        ) {
            val gap = lineGap.toPx()
            val stroke = gap * 0.16f
            WRITING_LINES.forEachIndexed { i, words ->
                val y = gap * (i + 1)
                drawLine(rule, Offset(0f, y - 1f), Offset(size.width, y - 1f), strokeWidth = 2f)
                // 줄마다 차례로: 이 줄이 채워진 정도(0~1).
                val lineProgress = (progress.value * WRITING_LINES.size - i).coerceIn(0f, 1f)
                var x = 0f
                val total = words.sum()
                var shown = lineProgress * total
                words.forEach { w ->
                    val width = size.width * w
                    val visible = (shown / w).coerceIn(0f, 1f) * width
                    if (visible > 0f) {
                        drawRoundRect(
                            color = ink.copy(alpha = 0.55f),
                            topLeft = Offset(x, y - gap * 0.42f),
                            size = Size(visible, stroke),
                            cornerRadius = CornerRadius(stroke / 2),
                        )
                    }
                    shown -= w
                    x += width + size.width * 0.025f
                }
            }
        }
        BasicText(
            "방금 하신 이야기는 이미 저장했어요",
            style = TextSpec(24f, FontWeight.SemiBold).style(AutoColors.InkMuted),
            modifier = Modifier.padding(top = 30.px),
        )
    }
}

@Composable
private fun SummaryContent(summary: InterviewSummary, onHome: () -> Unit, onSimilar: () -> Unit, modifier: Modifier) {
    val driving = LocalDriveMode.current == DriveMode.DRIVING
    val title = InterviewSession.titleOf(summary.chapter)
    val before = (summary.progressBefore * 100).roundToInt()
    val after = (summary.progressAfter * 100).roundToInt()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 40.px, top = 36.px, end = 40.px, bottom = 44.px),
    ) {
        StatusChipFor()
        RuledText(
            if (summary.chapterDone) "‘$title’${InterviewSession.objectParticle(title)} 완성했어요!\n오늘 이야기도 원고에 옮겼어요." else "오늘 이야기를\n원고에 옮겼어요.",
            Headline,
            Modifier.padding(top = if (driving) 110.px else 90.px).fillMaxWidth(),
        )
        // 진행률 변화: 71% → 86%
        Row(
            modifier = Modifier.padding(top = 50.px),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(20.px),
        ) {
            BasicText("$before%", style = Before.style(AutoColors.InkMuted), modifier = Modifier.padding(bottom = 14.px))
            BasicText("→", style = Arrow.style(AutoColors.InkMuted), modifier = Modifier.padding(bottom = 16.px))
            BasicText("$after%", style = After.style())
        }
        GrowthBar(summary.progressBefore, summary.progressAfter, Modifier.padding(top = 18.px).fillMaxWidth())
        BasicText(
            "${summary.chapter}장 $title · " + if (summary.remaining == 0) "챕터 완성" else "완성까지 질문 ${summary.remaining}개",
            style = (if (driving) Remaining else ChapterLine).style(AutoColors.InkSecondary),
            modifier = Modifier.padding(top = 18.px),
        )
        if (!driving) {
            Column(Modifier.padding(top = 40.px)) {
                HLine()
                StatRow("새로 적힌 문장", "${summary.sentences}문장")
                StatRow("대화 시간", formatDuration(summary.durationSec))
                StatRow("완성까지 남은 분량", if (summary.remaining == 0) "다 채웠어요" else "질문 ${summary.remaining}개")
            }
        }
        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(18.px)) {
            PillButton(
                text = "홈으로",
                onClick = onHome,
                modifier = Modifier.fillMaxWidth(),
                filled = true,
                heightPx = 128f,
                textSpec = HomeText,
                gapPx = 20f,
                leading = if (driving) {
                    {
                        RingTimer(
                            totalSeconds = SUMMARY_DRIVE_TIMEOUT_SECONDS,
                            running = LocalDemoOptions.current.timersRunning,
                            onFinished = onHome,
                            sizePx = 72f,
                            viewBoxPx = 72f,
                            radiusPx = 30f,
                            strokePx = 7f,
                            numberSizePx = 28f,
                            trackColor = RingTrackOnInk,
                            color = AutoColors.OnInk,
                        )
                    }
                } else {
                    null
                },
            )
            PillButton(
                text = "나와 비슷한 이야기 듣기",
                onClick = onSimilar,
                modifier = Modifier.fillMaxWidth(),
                icon = AutoIcons.Play,
                heightPx = 112f,
                iconSizePx = 30f,
                borderPx = 3f,
                textSpec = SimilarText,
            )
        }
    }
}

/** 지난 진행(옅은 잉크) 위에 이번에 채운 만큼(잉크)을 이어 그린다. */
@Composable
private fun GrowthBar(before: Float, after: Float, modifier: Modifier) {
    val track = AutoColors.Line
    val ink = AutoColors.Ink
    Canvas(modifier.height(18.px)) {
        val h = size.height
        drawLine(track, Offset(h / 2, h / 2), Offset(size.width - h / 2, h / 2), strokeWidth = h, cap = StrokeCap.Round)
        if (after > 0f) drawLine(ink, Offset(h / 2, h / 2), Offset(h / 2 + (size.width - h) * after, h / 2), strokeWidth = h, cap = StrokeCap.Round)
        if (before > 0f) drawLine(ink.copy(alpha = 0.35f).compositeOverWhite(), Offset(h / 2, h / 2), Offset(h / 2 + (size.width - h) * before, h / 2), strokeWidth = h, cap = StrokeCap.Round)
    }
}

/** 반투명 잉크를 흰 바탕 위 색으로(막대가 겹쳐도 같은 색이 되게). */
private fun Color.compositeOverWhite(): Color = Color(
    red = red * alpha + (1 - alpha),
    green = green * alpha + (1 - alpha),
    blue = blue * alpha + (1 - alpha),
)

@Composable
private fun StatRow(name: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 92.px),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(name, style = StatName.style(AutoColors.InkSecondary), modifier = Modifier.weight(1f))
        BasicText(value, style = StatValue.style())
    }
    HLine()
}

@Composable
private fun StatusChipFor() {
    val driving = LocalDriveMode.current == DriveMode.DRIVING
    ScreenStatusChip(if (driving) "주행 중 · 말로 불러 주세요" else "정차 중 · P", filled = driving, sizePx = 22f)
}

private fun formatDuration(sec: Int): String = when {
    sec < 60 -> "${sec}초"
    sec % 60 == 0 -> "${sec / 60}분"
    else -> "${sec / 60}분 ${sec % 60}초"
}

/** 노트 줄마다 글줄(낱말 폭 비율). */
private val WRITING_LINES = listOf(
    listOf(0.16f, 0.22f, 0.12f, 0.26f),
    listOf(0.24f, 0.14f, 0.3f, 0.1f),
    listOf(0.18f, 0.28f, 0.2f),
    listOf(0.22f, 0.16f),
)

/** 요약이 아직 없을 때(디버그로 화면만 열었을 때) 보일 예. */
private val DEMO_SUMMARY = InterviewSummary(chapter = 2, sentences = 4, durationSec = 192, answeredBefore = 5, answeredAfter = 6, total = 7, chapterDone = false)
