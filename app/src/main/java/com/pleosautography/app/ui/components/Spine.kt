package com.pleosautography.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.pleosautography.app.domain.DriveMode
import com.pleosautography.app.ui.icons.AutoIcons
import com.pleosautography.app.ui.theme.AutoColors
import com.pleosautography.app.ui.theme.AutoType
import com.pleosautography.app.ui.theme.DisabledAlpha
import com.pleosautography.app.ui.theme.LocalDriveMode
import com.pleosautography.app.ui.theme.px

/**
 * 세로 앱 화면 왼쪽의 72px 책등 띠.
 * 위에는 자서전 제목을 세로로, 아래에는 ‘Autography’ 로고와 설정 버튼(주행 중 비활성)을 둔다.
 * @param onLogoLongPress 디버그 빌드에서만: 로고를 길게 누르면 데모 패널을 연다.
 * @param settingsOpen 환경설정 화면이 열려 있으면 설정 버튼을 꽉 찬 원으로 그린다.
 */
@Composable
fun Spine(
    bookTitle: String,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onLogoLongPress: (() -> Unit)? = null,
    settingsOpen: Boolean = false,
) {
    Row(modifier.fillMaxHeight().width(72.px)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(top = 36.px, bottom = 28.px),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StackedTitle(bookTitle)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.px),
            ) {
                BasicText(
                    text = "Autography",
                    style = AutoType.SpineLogo.style(),
                    softWrap = false,
                    modifier = Modifier
                        .rotatedClockwise()
                        .then(
                            if (onLogoLongPress != null) {
                                Modifier.pointerInput(onLogoLongPress) { detectTapGestures(onLongPress = { onLogoLongPress() }) }
                            } else {
                                Modifier
                            },
                        ),
                )
                SettingsButton(onSettings, settingsOpen)
            }
        }
        VLine(color = AutoColors.Ink)
    }
}

/** 한글 세로쓰기: 글자를 바로 세운 채 위에서 아래로 쌓는다. 띄어쓰기는 약 0.3em. */
@Composable
private fun StackedTitle(title: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = title },
    ) {
        title.forEach { char ->
            if (char.isWhitespace()) {
                Spacer(Modifier.height(6.6.px))
            } else {
                BasicText(text = char.toString(), style = AutoType.SpineTitle.style())
            }
        }
    }
}

@Composable
private fun SettingsButton(onClick: () -> Unit, open: Boolean) {
    val enabled = LocalDriveMode.current == DriveMode.PARKED
    Box(
        modifier = Modifier
            .size(52.px)
            .alpha(if (enabled) 1f else DisabledAlpha)
            .clip(CircleShape)
            .then(if (open) Modifier.background(AutoColors.Ink) else Modifier.border(2.px, AutoColors.Ink, CircleShape))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = rememberVectorPainter(AutoIcons.Settings),
            contentDescription = "환경설정",
            colorFilter = ColorFilter.tint(if (open) AutoColors.OnInk else AutoColors.Ink),
            modifier = Modifier.size(26.px),
        )
    }
}
