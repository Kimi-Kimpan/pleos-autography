package com.pleosautography.app.ui.screens.drive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.pleosautography.app.data.demo.ChapterStatus
import com.pleosautography.app.data.demo.DemoData
import com.pleosautography.app.ui.components.PillButton
import com.pleosautography.app.ui.components.RingTimer
import com.pleosautography.app.ui.components.RuledText
import com.pleosautography.app.ui.components.StatusChip
import com.pleosautography.app.ui.theme.AutoColors
import com.pleosautography.app.ui.theme.DesignFrame
import com.pleosautography.app.ui.theme.GowunBatang
import com.pleosautography.app.ui.theme.LocalDemoOptions
import com.pleosautography.app.ui.theme.TextSpec
import com.pleosautography.app.ui.theme.px

private val Message = TextSpec(66f, FontWeight.Bold, GowunBatang, lineHeightPx = 94f, letterSpacingEm = -0.015f)
private val Saved = TextSpec(26f, FontWeight.SemiBold)
private val SaveAndStop = TextSpec(36f, FontWeight.ExtraBold)
private val Continue = TextSpec(34f, FontWeight.ExtraBold)

/** 시안의 링 타이머 바탕색. */
private val RingTrack = Color(0xFFD6D3F6)

/** 마무리 화면은 3초 뒤 자동 저장하고 주행 홈으로 돌아간다. */
const val PAUSE_TIMEOUT_SECONDS = 3

/**
 * 08 도착 전 마무리. 시안: design/screens/Pause.png
 * 책등 없이 창 전체를 파랗게 채운다. 가로 전체 창에서도 늘리지 않고 시안 폭 안에 둔다.
 */
@Composable
fun PauseScreen(onSaveAndStop: () -> Unit, onContinue: () -> Unit, modifier: Modifier = Modifier) {
    val chapter = DemoData.chapters.first { it.status == ChapterStatus.WRITING }
    val demo = DemoData.drive
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AutoColors.Ink),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = DesignFrame.WIDTH.px)
                .padding(start = 64.px, top = 72.px, end = 64.px, bottom = 64.px),
            verticalArrangement = Arrangement.spacedBy(40.px),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.px)) {
                StatusChip("도착까지 ${demo.pauseMinutesToArrival}분", sizePx = 24f, color = AutoColors.OnInk, horizontalPaddingPx = 18f, verticalPaddingPx = 6f)
                StatusChip(
                    "${chapter.number}장 ${chapter.title} · 질문 ${demo.pauseQuestionNumber}",
                    modifier = Modifier.alpha(0.85f),
                    sizePx = 24f,
                    color = AutoColors.OnInk,
                    horizontalPaddingPx = 18f,
                    verticalPaddingPx = 6f,
                )
            }
            RuledText(
                text = "도착이 곧이니,\n이따가 이어서\n작성을 도와드릴게요!",
                spec = Message,
                color = AutoColors.OnInk,
                ruleColor = AutoColors.RuledOnInk,
                modifier = Modifier
                    .padding(top = 60.px)
                    .fillMaxWidth(),
            )
            BasicText(
                "지금까지 하신 이야기는 모두 저장했어요.",
                style = Saved.style(AutoColors.OnInk),
                modifier = Modifier.alpha(0.85f),
            )
            Spacer(Modifier.weight(1f))
            Column(verticalArrangement = Arrangement.spacedBy(20.px)) {
                PillButton(
                    text = "여기까지 저장하고 다음에 이어하기",
                    onClick = onSaveAndStop,
                    modifier = Modifier.fillMaxWidth(),
                    filled = true,
                    heightPx = 156f,
                    startPaddingPx = 24f,
                    endPaddingPx = 44f,
                    gapPx = 22f,
                    textSpec = SaveAndStop,
                    baseColor = AutoColors.OnInk,
                    onBaseColor = AutoColors.Ink,
                    contentAlignment = Alignment.Start,
                    leading = {
                        RingTimer(
                            totalSeconds = PAUSE_TIMEOUT_SECONDS,
                            running = LocalDemoOptions.current.timersRunning,
                            onFinished = onSaveAndStop,
                            sizePx = 108f,
                            viewBoxPx = 108f,
                            radiusPx = 46f,
                            strokePx = 9f,
                            numberSizePx = 40f,
                            trackColor = RingTrack,
                        )
                    },
                )
                PillButton(
                    text = "계속하기",
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth(),
                    heightPx = 120f,
                    textSpec = Continue,
                    baseColor = AutoColors.OnInk,
                )
            }
        }
    }
}
