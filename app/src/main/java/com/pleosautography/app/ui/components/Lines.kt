package com.pleosautography.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import com.pleosautography.app.ui.theme.AutoColors
import com.pleosautography.app.ui.theme.px

/** 가로 구분선. 두께는 시안 px. */
@Composable
fun HLine(modifier: Modifier = Modifier, color: Color = AutoColors.Line, thicknessPx: Float = 2f) {
    Box(modifier.fillMaxWidth().height(thicknessPx.px).background(color))
}

/** 세로 구분선. 두께는 시안 px. */
@Composable
fun VLine(modifier: Modifier = Modifier, color: Color = AutoColors.Ink, thicknessPx: Float = 2f) {
    Box(modifier.fillMaxHeight().width(thicknessPx.px).background(color))
}

/** 글자를 시계 방향으로 90° 돌려 세로로 세운다(CSS writing-mode: vertical-rl의 라틴 문자처럼). */
fun Modifier.rotatedClockwise(): Modifier = layout { measurable, _ ->
    val placeable = measurable.measure(Constraints())
    layout(placeable.height, placeable.width) {
        placeable.placeWithLayer(
            x = (placeable.height - placeable.width) / 2,
            y = (placeable.width - placeable.height) / 2,
        ) { rotationZ = 90f }
    }
}
