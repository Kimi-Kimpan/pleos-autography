package com.pleosautography.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import com.pleosautography.app.R

@OptIn(ExperimentalTextApi::class)
private fun astaSans(weight: Int) = Font(
    R.font.asta_sans,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** UI 전반. 가변 글꼴(300–800). */
val AstaSans = FontFamily(
    astaSans(300), astaSans(400), astaSans(500), astaSans(600), astaSans(700), astaSans(800),
)

/** 오토의 말, 원고, 질문, ‘Autography’ 로고. */
val GowunBatang = FontFamily(
    Font(R.font.gowun_batang_regular, FontWeight.Normal),
    Font(R.font.gowun_batang_bold, FontWeight.Bold),
)

/**
 * 시안 px 단위의 글자 스타일. [style]로 현재 창 배율에 맞는 TextStyle을 만든다.
 * lineHeightPx를 주면 CSS처럼 줄 상자 가운데에 글자를 놓는다(노트 줄과 맞추기 위해).
 */
data class TextSpec(
    val sizePx: Float,
    val weight: FontWeight,
    val family: FontFamily = AstaSans,
    val lineHeightPx: Float? = null,
    val letterSpacingEm: Float = 0f,
    val tabularNumbers: Boolean = false,
) {
    @Composable
    @ReadOnlyComposable
    fun style(color: Color = AutoColors.Ink): TextStyle = TextStyle(
        color = color,
        fontFamily = family,
        fontWeight = weight,
        fontSize = sizePx.pxSp,
        lineHeight = lineHeightPx?.pxSp ?: TextUnit.Unspecified,
        lineHeightStyle = lineHeightPx?.let {
            LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
        },
        letterSpacing = letterSpacingEm.em,
        fontFeatureSettings = if (tabularNumbers) "tnum" else null,
    )
}

/** 시안에서 쓰는 글자 스타일 모음(design/html 값 기준). */
object AutoType {
    // 책등. 세로쓰기 한 글자의 높이는 시안에서 약 1.2em이다.
    val SpineTitle = TextSpec(22f, FontWeight.ExtraBold, lineHeightPx = 26.4f)
    val SpineLogo = TextSpec(21f, FontWeight.Bold, GowunBatang, letterSpacingEm = 0.03f)

    // 오토의 말(명조 + 노트 줄)
    val GreetingParked = TextSpec(45f, FontWeight.Bold, GowunBatang, lineHeightPx = 64f, letterSpacingEm = -0.01f)

    // 챕터 목록
    val ChapterNumber = TextSpec(28f, FontWeight.Bold, tabularNumbers = true)
    val ChapterTitle = TextSpec(40f, FontWeight.Bold)
    val ChapterTitleExpanded = TextSpec(52f, FontWeight.ExtraBold, lineHeightPx = 57.2f)
    val Meta = TextSpec(24f, FontWeight.SemiBold)
    val LastStory = TextSpec(24f, FontWeight.SemiBold, lineHeightPx = 54f)

    // 컨트롤
    val Chip = TextSpec(20f, FontWeight.Bold, lineHeightPx = 23f)
    val Pill = TextSpec(30f, FontWeight.ExtraBold)

    // 미니 플레이어
    val PlayerLabel = TextSpec(21f, FontWeight.Bold)
    val PlayerTitle = TextSpec(30f, FontWeight.ExtraBold)
    val PlayerMeta = TextSpec(21f, FontWeight.SemiBold)
    val CoverText = TextSpec(12f, FontWeight.ExtraBold, lineHeightPx = 13.2f)

    // 자리표시 화면(마일스톤 2에서 실제 화면으로 바뀐다)
    val PlaceholderTitle = TextSpec(45f, FontWeight.Bold, GowunBatang, lineHeightPx = 64f)
}
