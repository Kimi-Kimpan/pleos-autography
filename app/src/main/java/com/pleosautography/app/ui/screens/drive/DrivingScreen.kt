package com.pleosautography.app.ui.screens.drive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.pleosautography.app.data.demo.ChapterStatus
import com.pleosautography.app.data.demo.DemoData
import com.pleosautography.app.ui.components.BigMicButton
import com.pleosautography.app.ui.components.MiniPlayer
import com.pleosautography.app.ui.components.ProgressBar
import com.pleosautography.app.ui.components.RuledText
import com.pleosautography.app.ui.components.SingleLine
import com.pleosautography.app.ui.components.StatusChip
import com.pleosautography.app.ui.theme.AutoColors
import com.pleosautography.app.ui.theme.DriveMode
import com.pleosautography.app.ui.theme.GowunBatang
import com.pleosautography.app.ui.theme.LocalDemoOptions
import com.pleosautography.app.ui.theme.LocalDriveMode
import com.pleosautography.app.ui.theme.TextSpec
import com.pleosautography.app.ui.theme.px

private val Greeting = TextSpec(58f, FontWeight.Bold, GowunBatang, lineHeightPx = 84f, letterSpacingEm = -0.015f)
private val LastStory = TextSpec(28f, FontWeight.SemiBold)
private val CallHint = TextSpec(32f, FontWeight.Bold, GowunBatang, lineHeightPx = 48f)

/**
 * 02 주행 중 홈. 시안: design/screens/Driving.png
 * 내비와 나란히 놓인 세로 창에 뜨면 06(NavToolbar) 화면이 된다.
 */
@Composable
fun DrivingScreen(onCallOto: () -> Unit, onOpenPlayer: () -> Unit, modifier: Modifier = Modifier) {
    val chapter = DemoData.chapters.first { it.status == ChapterStatus.WRITING }
    val pulse = LocalDriveMode.current == DriveMode.PARKED || LocalDemoOptions.current.micPulseWhileDriving
    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 40.px, top = 36.px, end = 40.px, bottom = 44.px),
        ) {
            StatusChip("주행 중 · 말로 불러 주세요", filled = true)
            RuledText(
                text = "좋은 하루 되세요!\n‘${chapter.title}’ 부분을 쓰고 있어요.",
                spec = Greeting,
                modifier = Modifier
                    .padding(top = 150.px)
                    .fillMaxWidth(),
            )
            ProgressBar(
                progress = chapter.progress,
                heightPx = 14f,
                modifier = Modifier
                    .padding(top = 34.px)
                    .fillMaxWidth(0.66f),
            )
            chapter.lastStory?.let {
                SingleLine("마지막 이야기: $it", LastStory, AutoColors.InkMuted, Modifier.padding(top = 18.px))
            }
            Spacer(Modifier.weight(1f))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(44.px),
            ) {
                BigMicButton(pulse = pulse, onClick = onCallOto, modifier = Modifier.padding(end = 40.px))
                BasicText(
                    text = "챕터를 이어 쓰시려면\n“오토야, 뭐해?”라고 말씀해 주세요",
                    style = CallHint.style().copy(textAlign = TextAlign.End),
                )
            }
        }
        MiniPlayer(DemoData.nowPlaying, onExpand = onOpenPlayer)
    }
}
