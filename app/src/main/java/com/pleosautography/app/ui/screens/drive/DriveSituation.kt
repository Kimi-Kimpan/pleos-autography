package com.pleosautography.app.ui.screens.drive

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.pleosautography.app.data.demo.DemoData
import com.pleosautography.app.domain.trigger.DriveNotice
import com.pleosautography.app.domain.trigger.NudgeReason

/**
 * 주행 화면이 보여 줄 지금 상황(SituationController에서 온다).
 * @param minutesToArrival 목적지까지 남은 분. 경로 안내가 없으면 null.
 * @param nudgeReason 제안 화면의 이유(문구와 칩).
 * @param notice 주행 홈 인사말 자리에 잠시 보일 안내(미루기·휴식).
 */
@Immutable
data class DriveSituation(
    val minutesToArrival: Int? = DemoData.drive.minutesToArrival,
    val nudgeReason: NudgeReason = NudgeReason.CONGESTION,
    val notice: DriveNotice? = null,
)

val LocalDriveSituation = staticCompositionLocalOf { DriveSituation() }
