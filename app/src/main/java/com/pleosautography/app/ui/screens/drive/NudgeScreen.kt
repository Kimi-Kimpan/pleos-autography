package com.pleosautography.app.ui.screens.drive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.pleosautography.app.data.demo.ChapterStatus
import com.pleosautography.app.data.demo.DemoData
import com.pleosautography.app.domain.trigger.NudgeReason
import com.pleosautography.app.ui.components.PillButton
import com.pleosautography.app.ui.components.ProgressBar
import com.pleosautography.app.ui.components.RingTimer
import com.pleosautography.app.ui.components.RuledText
import com.pleosautography.app.ui.components.StatusChip
import com.pleosautography.app.ui.icons.AutoIcons
import com.pleosautography.app.ui.theme.AutoColors
import com.pleosautography.app.ui.theme.GowunBatang
import com.pleosautography.app.ui.theme.LocalDemoOptions
import com.pleosautography.app.ui.theme.TextSpec
import com.pleosautography.app.ui.theme.px
import kotlin.math.roundToInt

private val Suggestion = TextSpec(54f, FontWeight.Bold, GowunBatang, lineHeightPx = 78f, letterSpacingEm = -0.015f)
private val ProgressLabel = TextSpec(26f, FontWeight.Bold, tabularNumbers = true)
private val Primary = TextSpec(40f, FontWeight.ExtraBold)
private val Later = TextSpec(36f, FontWeight.ExtraBold)
private val VoiceHint = TextSpec(24f, FontWeight.SemiBold)

/** 제안 팝업은 5초 동안 응답이 없으면 스스로 닫힌다. */
const val NUDGE_TIMEOUT_SECONDS = 5

/**
 * 07 정체 감지 제안. 시안: design/screens/Nudge.png (내비 옆 세로 창)
 * 알림 멜로디 뒤에 뜬다. 내비 위 오버레이가 아니라 오토그래피 창 안에 그린다.
 */
@Composable
fun NudgeScreen(onAccept: () -> Unit, onLater: () -> Unit, modifier: Modifier = Modifier) {
    val chapter = DemoData.chapters.first { it.status == ChapterStatus.WRITING }
    val percent = (chapter.progress * 100).roundToInt()
    val reason = LocalDriveSituation.current.nudgeReason
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 40.px, top = 36.px, end = 40.px, bottom = 44.px),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.px)) {
            StatusChip("알림 멜로디", filled = true, sizePx = 22f, icon = AutoIcons.Note)
            StatusChip(reason.chip(), sizePx = 22f)
        }
        RuledText(
            text = if (reason == NudgeReason.RESUME) {
                "복잡한 구간을 지났어요.\n아까 ‘${chapter.title}’ 이야기,\n이어서 해 볼까요?"
            } else {
                "지난번 ‘${chapter.title}’ 이야기,\n이어서 해 볼까요?\n조금만 더 채워 주시면\n이 챕터가 완성돼요!"
            },
            spec = Suggestion,
            modifier = Modifier
                .padding(top = 150.px)
                .fillMaxWidth(),
        )
        Column(
            modifier = Modifier.padding(top = 48.px),
            verticalArrangement = Arrangement.spacedBy(14.px),
        ) {
            Row(Modifier.fillMaxWidth()) {
                BasicText("${chapter.number}장 ${chapter.title}", style = ProgressLabel.style(), modifier = Modifier.weight(1f))
                BasicText("$percent% · 완성까지 ${100 - percent}%", style = ProgressLabel.style())
            }
            ProgressBar(chapter.progress, heightPx = 16f)
        }
        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(18.px)) {
            PillButton(
                text = "이어서 하기",
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth(),
                icon = AutoIcons.MicFilled,
                filled = true,
                heightPx = 128f,
                iconSizePx = 44f,
                gapPx = 16f,
                textSpec = Primary,
            )
            PillButton(
                text = "나중에",
                onClick = onLater,
                modifier = Modifier.fillMaxWidth(),
                heightPx = 112f,
                borderPx = 3f,
                gapPx = 18f,
                textSpec = Later,
                leading = {
                    RingTimer(
                        totalSeconds = NUDGE_TIMEOUT_SECONDS,
                        running = LocalDemoOptions.current.timersRunning,
                        onFinished = onLater,
                        sizePx = 64f,
                        viewBoxPx = 70f,
                        radiusPx = 29f,
                        strokePx = 7f,
                        numberSizePx = 26f,
                    )
                },
            )
            BasicText(
                text = "“응”, “이어서 하자”라고 말해도 시작해요",
                style = VoiceHint.style(AutoColors.InkSecondary),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 6.px),
            )
        }
    }
}

private fun NudgeReason.chip(): String = when (this) {
    NudgeReason.CONGESTION -> "정체 구간 ${DemoData.drive.congestionMinutes}분째"
    NudgeReason.STRAIGHT -> "긴 직진 ${DemoData.drive.congestionMinutes}분째"
    NudgeReason.RESUME -> "다시 이어 하기"
}
