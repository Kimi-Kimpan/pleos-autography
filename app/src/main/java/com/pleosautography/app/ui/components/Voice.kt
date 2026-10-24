package com.pleosautography.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.pleosautography.app.ui.icons.AutoIcons
import com.pleosautography.app.ui.theme.AutoColors
import com.pleosautography.app.ui.theme.TextSpec
import com.pleosautography.app.ui.theme.px
import kotlin.math.ceil

/**
 * 주행 홈의 큰 마이크: 220px 파란 원 + 40px 후광. 주행 중 자동 애니메이션을 줄이기 위해 움직이는 효과는 없다.
 */
@Composable
fun BigMicButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val halo = 40.px
    Box(
        modifier = modifier
            .size(220.px)
            .drawBehind {
                drawCircle(AutoColors.MicHalo, radius = size.minDimension / 2 + halo.toPx())
            }
            .clip(CircleShape)
            .background(AutoColors.Ink)
            .clickable(role = Role.Button, onClickLabel = "오토 호출", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = rememberVectorPainter(AutoIcons.MicFilled),
            contentDescription = "오토 호출",
            colorFilter = ColorFilter.tint(AutoColors.OnInk),
            modifier = Modifier.size(88.px),
        )
    }
}

/**
 * ‘듣고 있어요’ 음성 파형. 막대 10개(기본 폭 16, 간격 14, 높이 190).
 * 주행 중 자동 애니메이션을 줄이기 위해 지금은 멈춘 모양이고, 마일스톤 5에서 입력 음량에 맞춰 움직인다.
 * @param levels 막대마다 0~1 높이 배율.
 */
@Composable
fun VoiceWave(
    modifier: Modifier = Modifier,
    levels: List<Float> = RestingWave,
    barWidthPx: Float = 16f,
    gapPx: Float = 14f,
    heightPx: Float = 190f,
) {
    Row(
        modifier = modifier.height(heightPx.px),
        horizontalArrangement = Arrangement.spacedBy(gapPx.px),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        levels.forEach { level ->
            Box(
                Modifier
                    .width(barWidthPx.px)
                    .height((heightPx * level.coerceIn(0.1f, 1f)).px)
                    .clip(RoundedCornerShape((barWidthPx / 2).px))
                    .background(AutoColors.Ink),
            )
        }
    }
}

/** 시안의 막대 높이(44~186px) / 190. */
private val RestingWave = listOf(44f, 92f, 150f, 96f, 186f, 140f, 170f, 86f, 120f, 48f).map { it / 190f }

/**
 * 남은 시간을 링으로 보여 주는 타이머. 남은 비율만큼 호를 그리고 가운데에 남은 초를 쓴다.
 * [running]이 false면 멈춘 채 시작 상태로 보인다.
 *
 * @param viewBoxPx 시안 SVG의 viewBox 한 변, [radiusPx]와 [strokePx]도 그 좌표계 값이다.
 */
@Composable
fun RingTimer(
    totalSeconds: Int,
    running: Boolean,
    onFinished: () -> Unit,
    sizePx: Float,
    viewBoxPx: Float,
    radiusPx: Float,
    strokePx: Float,
    numberSizePx: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = AutoColors.Line,
    color: Color = AutoColors.Ink,
) {
    val elapsed = remember { Animatable(0f) }
    val finish by rememberUpdatedState(onFinished)
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        elapsed.animateTo(1f, tween(totalSeconds * 1000, easing = LinearEasing))
        finish()
    }
    val remaining = ceil(totalSeconds * (1f - elapsed.value)).toInt().coerceAtLeast(0)
    Box(modifier.size(sizePx.px), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(sizePx.px)) {
            val unit = size.minDimension / viewBoxPx
            val stroke = Stroke(strokePx * unit, cap = StrokeCap.Round)
            val radius = radiusPx * unit
            val topLeft = center - Offset(radius, radius)
            val arcSize = Size(radius * 2, radius * 2)
            drawCircle(trackColor, radius, style = Stroke(strokePx * unit))
            drawArc(color, -90f, 360f * (1f - elapsed.value), false, topLeft, arcSize, style = stroke)
        }
        BasicText(
            text = remaining.toString(),
            style = TextSpec(numberSizePx, FontWeight.ExtraBold, tabularNumbers = true).style(color),
        )
    }
}
