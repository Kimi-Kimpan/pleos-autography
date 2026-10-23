package com.pleosautography.app.ui.theme

import androidx.compose.ui.graphics.Color

/** design/tokens.md의 색 토큰. */
object AutoColors {
    val Bg = Color(0xFFEFEFEF)
    val SurfaceRaised = Color(0xFFF7F7F7)
    val SurfacePill = Color(0xFFF8F8F8)
    val Ink = Color(0xFF1A0BD0)
    val InkSecondary = Color(0xFF4A44A6)
    val InkMuted = Color(0xFF7A74C9)
    val Line = Color(0xFFC4C3D8)
    val Ruled = Color(0xFFC9C7DE)
    val LavenderSoft = Color(0xFFD6D2F8)
    val OnInk = Color(0xFFFFFFFF)

    /** 파란 면 위의 노트 줄. */
    val RuledOnInk = Color(0x52FFFFFF)

    /** 큰 마이크 바깥 후광. */
    val MicHalo = Color(0x1A1A0BD0)

    /** 미니 플레이어 표지 종이색(시안 값). */
    val CoverPaper = Color(0xFFF3F0EA)
}

/** 주행 중 막힌 컨트롤의 불투명도. */
const val DisabledAlpha = 0.32f
