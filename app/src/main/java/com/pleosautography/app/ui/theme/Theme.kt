package com.pleosautography.app.ui.theme

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/** 주행/정차 상태. 마일스톤 3에서 VehicleState가 이 값을 정한다. */
enum class DriveMode { PARKED, DRIVING }

val LocalDriveMode = staticCompositionLocalOf { DriveMode.PARKED }

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
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalDesignScale provides designScale,
        LocalDriveMode provides driveMode,
        LocalTextSelectionColors provides SelectionColors,
        content = content,
    )
}
