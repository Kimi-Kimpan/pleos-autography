package com.pleosautography.app.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import com.pleosautography.app.ui.theme.AutoColors
import com.pleosautography.app.ui.theme.TextSpec
import com.pleosautography.app.ui.theme.px

/**
 * 노트 줄 위에 쓴 명조 문장. 줄마다 줄 상자 바닥에 2px 선을 긋고, 선은 텍스트 영역 끝까지 이어진다.
 * [spec]에는 lineHeightPx가 있어야 한다.
 */
@Composable
fun RuledText(
    text: String,
    spec: TextSpec,
    modifier: Modifier = Modifier,
    color: Color = AutoColors.Ink,
    ruleColor: Color = AutoColors.Ruled,
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val ruleThickness = 2.px
    BasicText(
        text = text,
        style = spec.style(color),
        onTextLayout = { layout = it },
        modifier = modifier.drawBehind {
            val result = layout ?: return@drawBehind
            val thickness = ruleThickness.toPx()
            for (line in 0 until result.lineCount) {
                drawRect(
                    color = ruleColor,
                    topLeft = Offset(0f, result.getLineBottom(line) - thickness),
                    size = Size(size.width, thickness),
                )
            }
        },
    )
}
