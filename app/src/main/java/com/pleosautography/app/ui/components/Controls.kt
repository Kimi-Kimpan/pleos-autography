package com.pleosautography.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.pleosautography.app.ui.theme.AutoColors
import com.pleosautography.app.ui.theme.AutoType
import com.pleosautography.app.ui.theme.DisabledAlpha
import com.pleosautography.app.ui.theme.px

private val PillShape = RoundedCornerShape(percent = 50)

/**
 * 알약 버튼. 파란 꽉 찬 모양([filled])은 쓰기·말하기 행동(이어쓰기, 이어서 하기)에만 쓴다.
 * 폭을 weight로 나눠 쓸 때는 contentPadding을 0으로 두면 내용이 가운데 놓인다.
 */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    filled: Boolean = false,
    enabled: Boolean = true,
    heightPx: Float = 84f,
    iconSizePx: Float = 28f,
    horizontalPaddingPx: Float = 34f,
) {
    val contentColor = if (filled) AutoColors.OnInk else AutoColors.Ink
    Row(
        modifier = modifier
            .height(heightPx.px)
            .alpha(if (enabled) 1f else DisabledAlpha)
            .clip(PillShape)
            .then(if (filled) Modifier.background(AutoColors.Ink) else Modifier)
            .border(2.5.px, AutoColors.Ink, PillShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(PaddingValues(horizontal = horizontalPaddingPx.px)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.px, Alignment.CenterHorizontally),
    ) {
        if (icon != null) {
            Image(
                painter = rememberVectorPainter(icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(contentColor),
                modifier = Modifier.size(iconSizePx.px),
            )
        }
        BasicText(
            text = text,
            style = AutoType.Pill.style(contentColor),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
    }
}

/** 원형 아이콘 버튼. 기본 80px, 아웃라인 2.5px. */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    sizePx: Float = 80f,
    iconSizePx: Float = 32f,
    borderPx: Float = 2.5f,
) {
    Box(
        modifier = modifier
            .size(sizePx.px)
            .alpha(if (enabled) 1f else DisabledAlpha)
            .clip(CircleShape)
            .border(borderPx.px, AutoColors.Ink, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = rememberVectorPainter(icon),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(AutoColors.Ink),
            modifier = Modifier.size(iconSizePx.px),
        )
    }
}

/** 상태 칩(예: “정차 중 · P”). [filled]는 주행 중 칩. */
@Composable
fun StatusChip(text: String, modifier: Modifier = Modifier, filled: Boolean = false) {
    BasicText(
        text = text,
        style = AutoType.Chip.style(if (filled) AutoColors.OnInk else AutoColors.Ink),
        modifier = modifier
            .clip(PillShape)
            .then(if (filled) Modifier.background(AutoColors.Ink) else Modifier)
            .border(2.px, AutoColors.Ink, PillShape)
            // Compose의 border는 공간을 차지하지 않으므로 시안(CSS)의 테두리 2px를 안쪽 여백에 더한다.
            .padding(horizontal = 18.px, vertical = 6.px),
    )
}

/** 진행바. 바탕 line, 채움 ink. */
@Composable
fun ProgressBar(progress: Float, modifier: Modifier = Modifier, heightPx: Float = 12f) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heightPx.px)
            .clip(PillShape)
            .background(AutoColors.Line),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(AutoColors.Ink),
        )
    }
}
