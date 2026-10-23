package com.pleosautography.app.ui.theme

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.pleosautography.app.domain.DriveMode

val LocalDriveMode = staticCompositionLocalOf { DriveMode.PARKED }

/**
 * 시연용 설정.
 * @param timersRunning 링 타이머(제안 5초, 마무리 3초)가 흐르는지. 시안 비교 스크린샷을 찍을 때만 끈다.
 */
@Immutable
data class DemoOptions(
    val timersRunning: Boolean = true,
)

val LocalDemoOptions = staticCompositionLocalOf { DemoOptions() }

/**
 * 화면 안의 상태 칩(“정차 중 · P”, “주행 중 · …”)을 그릴지. 가로 전체 창에서는 창 오른쪽 위 한 곳에만
 * 칩을 그리므로 false가 된다(docs/screens.md ‘상태 칩’).
 */
val LocalShowStatusChip = staticCompositionLocalOf { true }

private val SelectionColors = TextSelectionColors(
    handleColor = AutoColors.Ink,
    backgroundColor = AutoColors.LavenderSoft,
)

/**
 * @param designScale 시안 1px이 몇 dp인지(창 높이 / 1280).
 */
@Composable
fun AutographyTheme(
    designScale: Float,
    driveMode: DriveMode = DriveMode.PARKED,
    demoOptions: DemoOptions = DemoOptions(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalDesignScale provides designScale,
        LocalDriveMode provides driveMode,
        LocalDemoOptions provides demoOptions,
        LocalTextSelectionColors provides SelectionColors,
        content = content,
    )
}
