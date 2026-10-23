package com.pleosautography.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/**
 * 시안 좌표계. 세로 앱 화면 시안은 960×1280 px이다.
 * 시안 1px = 창 높이 / 1280 dp로 환산하고, 가로는 창 폭에 맞춰 반응형으로 늘이거나 줄인다.
 */
object DesignFrame {
    const val WIDTH = 960f
    const val HEIGHT = 1280f
}

/** 시안 1px이 몇 dp인지. AutographyTheme이 창 높이로 계산해 넣는다. */
val LocalDesignScale = staticCompositionLocalOf { 1f }

/** 시안 px → dp. */
val Number.px: Dp
    @Composable @ReadOnlyComposable
    get() = (toFloat() * LocalDesignScale.current).dp

/**
 * 시안 px → sp. 시안 비율이 깨지지 않도록 시스템 글자 크기 배율은 적용하지 않는다.
 */
val Number.pxSp: TextUnit
    @Composable @ReadOnlyComposable
    get() = with(LocalDensity.current) { (toFloat() * LocalDesignScale.current).dp.toSp() }
