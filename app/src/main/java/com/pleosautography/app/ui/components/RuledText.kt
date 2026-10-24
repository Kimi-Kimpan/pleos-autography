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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import com.pleosautography.app.ui.theme.AutoColors
import com.pleosautography.app.ui.theme.TextSpec
import com.pleosautography.app.ui.theme.px

private val KoreanLineBreak = LineBreak(LineBreak.Strategy.Simple, LineBreak.Strictness.Normal, LineBreak.WordBreak.Phrase)

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
    textAlign: TextAlign = TextAlign.Start,
) = RuledText(AnnotatedString(text), spec, modifier, color, ruleColor, textAlign)

/** 문장 일부에 취소선·강조를 넣을 때(원고 고치기). */
@Composable
fun RuledText(
    text: AnnotatedString,
    spec: TextSpec,
    modifier: Modifier = Modifier,
    color: Color = AutoColors.Ink,
    ruleColor: Color = AutoColors.Ruled,
    textAlign: TextAlign = TextAlign.Start,
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val ruleThickness = 2.px
    BasicText(
        text = text,
        // 한국어는 어절 단위로 줄을 바꾼다(CSS word-break: keep-all처럼). 길게 생성된 후속 질문도 단어 중간에서 끊기지 않는다.
        style = spec.style(color).copy(textAlign = textAlign, lineBreak = KoreanLineBreak),
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
