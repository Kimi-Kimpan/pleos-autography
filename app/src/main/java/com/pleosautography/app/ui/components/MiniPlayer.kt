package com.pleosautography.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.pleosautography.app.data.demo.NowPlaying
import com.pleosautography.app.domain.DriveMode
import com.pleosautography.app.ui.icons.AutoIcons
import com.pleosautography.app.ui.screens.LocalRadioPlayback
import com.pleosautography.app.ui.theme.AutoColors
import com.pleosautography.app.ui.theme.AutoType
import com.pleosautography.app.ui.theme.LocalDriveMode
import com.pleosautography.app.ui.theme.TextSpec
import com.pleosautography.app.ui.theme.px

/**
 * 하단 미니 플레이어(위에 파란 2px 선 포함).
 * 일시정지·다음은 주행 중에도 쓰고, 펼치기(⌃)는 정차 중에만 쓴다.
 * @param pausedForInterview 인터뷰가 열려 라디오를 잠시 멈춘 상태(재생 버튼과 “인터뷰 중 잠시 멈춤”).
 */
@Composable
fun MiniPlayer(
    nowPlaying: NowPlaying,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    pausedForInterview: Boolean = false,
) {
    val parked = LocalDriveMode.current == DriveMode.PARKED
    Column(modifier) {
        HLine(color = AutoColors.Ink)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.px, top = 26.px, end = 32.px, bottom = 32.px),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.px),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 84.px, height = 112.px)
                    .background(AutoColors.CoverPaper)
                    .border(1.5.px, AutoColors.Ink)
                    .padding(9.5.px),
                contentAlignment = Alignment.BottomStart,
            ) {
                BasicText("Driving\nTogether", style = AutoType.CoverText.style())
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.px),
            ) {
                SingleLine(
                    when {
                        pausedForInterview -> "오토그래피 라디오 · 인터뷰 중 잠시 멈춤"
                        // 알림 멜로디·오토 발화 중에는 소리를 줄인다.
                        LocalRadioPlayback.current.ducked -> "오토그래피 라디오 · 소리 줄임"
                        else -> nowPlaying.label
                    },
                    AutoType.PlayerLabel,
                    AutoColors.InkSecondary,
                )
                SingleLine(nowPlaying.title, AutoType.PlayerTitle, AutoColors.Ink)
                SingleLine(nowPlaying.meta, AutoType.PlayerMeta, AutoColors.InkSecondary)
            }
            CircleIconButton(
                if (pausedForInterview) AutoIcons.Play else AutoIcons.Pause,
                if (pausedForInterview) "재생" else "일시정지",
                onClick = {},
                sizePx = 92f,
                iconSizePx = 34f,
                borderPx = 3.5f,
            )
            CircleIconButton(AutoIcons.Next, "다음 이야기", onClick = {})
            VLine(Modifier.height(64.px), color = AutoColors.Line)
            CircleIconButton(
                AutoIcons.ChevronUp,
                if (parked) "플레이어 펼치기" else "플레이어 펼치기 (주행 중 사용 불가)",
                onClick = onExpand,
                enabled = parked,
                iconSizePx = 36f,
            )
        }
    }
}

/** 한 줄, 넘치면 말줄임. */
@Composable
fun SingleLine(text: String, spec: TextSpec, color: Color, modifier: Modifier = Modifier) {
    BasicText(
        text = text,
        style = spec.style(color),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}
