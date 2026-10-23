package com.pleosautography.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.pleosautography.app.ui.icons.AutoIcons
import com.pleosautography.app.ui.theme.AutoColors
import com.pleosautography.app.ui.theme.AutoType
import com.pleosautography.app.ui.theme.DisabledAlpha
import com.pleosautography.app.ui.theme.TextSpec
import com.pleosautography.app.ui.theme.px

private val PillShape = RoundedCornerShape(percent = 50)

/**
 * 알약 버튼. 파란 꽉 찬 모양([filled])은 쓰기·말하기 행동(이어쓰기, 이어서 하기)에만 쓴다.
 * 폭을 weight로 나눠 쓸 때는 horizontalPaddingPx를 0으로 두면 내용이 가운데 놓인다.
 *
 * @param baseColor 꽉 찬 모양이면 바탕색, 아웃라인이면 테두리·글자색.
 * @param onBaseColor 꽉 찬 모양의 글자색.
 * @param leading 아이콘 대신 앞에 둘 내용(예: 링 타이머).
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
    startPaddingPx: Float = horizontalPaddingPx,
    endPaddingPx: Float = horizontalPaddingPx,
    gapPx: Float = 12f,
    textSpec: TextSpec = AutoType.Pill,
    borderPx: Float = 2.5f,
    baseColor: Color = AutoColors.Ink,
    onBaseColor: Color = AutoColors.OnInk,
    contentAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    leading: (@Composable () -> Unit)? = null,
) {
    val contentColor = if (filled) onBaseColor else baseColor
    Row(
        modifier = modifier
            .height(heightPx.px)
            .alpha(if (enabled) 1f else DisabledAlpha)
            .clip(PillShape)
            .then(if (filled) Modifier.background(baseColor) else Modifier)
            .border(borderPx.px, baseColor, PillShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(start = startPaddingPx.px, end = endPaddingPx.px),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(gapPx.px, contentAlignment),
    ) {
        if (leading != null) {
            leading()
        } else if (icon != null) {
            Image(
                painter = rememberVectorPainter(icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(contentColor),
                modifier = Modifier.size(iconSizePx.px),
            )
        }
        BasicText(
            text = text,
            style = textSpec.style(contentColor),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
    }
}

/** ‘여기까지’(저장 후 중지) 버튼: 클립 아이콘이 들어간 아웃라인 알약. 주행 중에도 쓸 수 있다. */
@Composable
fun StopHereButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    PillButton(
        text = "여기까지",
        onClick = onClick,
        modifier = modifier,
        icon = AutoIcons.Clip,
        heightPx = 104f,
        iconSizePx = 36f,
        startPaddingPx = 30f,
        endPaddingPx = 40f,
        gapPx = 14f,
        textSpec = TextSpec(34f, FontWeight.Bold),
    )
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

/**
 * 상태 칩(예: “정차 중 · P”, “주행 중 · 인터뷰”).
 * 꽉 찬 칩은 테두리 없이 안쪽 여백 5px, 아웃라인 칩은 2px 테두리 + 4px 여백이다(시안 CSS 기준).
 * Compose의 border는 공간을 차지하지 않으므로 아웃라인 칩은 테두리 두께를 여백에 더한다.
 */
@Composable
fun StatusChip(
    text: String,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    sizePx: Float = 20f,
    icon: ImageVector? = null,
    color: Color = AutoColors.Ink,
    onColor: Color = AutoColors.OnInk,
    horizontalPaddingPx: Float = 16f,
    verticalPaddingPx: Float = if (filled) 5f else 4f,
) {
    val contentColor = if (filled) onColor else color
    val inset = if (filled) 0f else 2f
    Row(
        modifier = modifier
            .clip(PillShape)
            .then(if (filled) Modifier.background(color) else Modifier.border(2.px, color, PillShape))
            .padding(horizontal = (horizontalPaddingPx + inset).px, vertical = (verticalPaddingPx + inset).px),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.px),
    ) {
        if (icon != null) {
            Image(
                painter = rememberVectorPainter(icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(contentColor),
                modifier = Modifier.size(sizePx.px),
            )
        }
        BasicText(
            text = text,
            style = TextSpec(sizePx, FontWeight.Bold, lineHeightPx = sizePx * 1.15f).style(contentColor),
            maxLines = 1,
            softWrap = false,
        )
    }
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
