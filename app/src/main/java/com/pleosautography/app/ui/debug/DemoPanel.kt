package com.pleosautography.app.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import com.pleosautography.app.domain.demo.DemoEvent
import com.pleosautography.app.ui.components.PillButton
import com.pleosautography.app.ui.theme.AutoColors
import com.pleosautography.app.ui.theme.TextSpec
import com.pleosautography.app.ui.theme.px

private val PanelTitle = TextSpec(22f, FontWeight.ExtraBold)
private val PanelButton = TextSpec(22f, FontWeight.Bold)

/** 마일스톤 6에서 처리하기 전까지 누를 수 없는 이벤트. */
private val PendingEvents = setOf(
    DemoEvent.CONGESTION_15, DemoEvent.STRAIGHT_15, DemoEvent.COMPLEX_START, DemoEvent.COMPLEX_END, DemoEvent.ARRIVAL_10,
)

/**
 * 디버그 빌드 전용 데모 패널. 책등의 ‘Autography’를 길게 누르면 열린다.
 * 시연 영상에는 보이지 않게, 같은 이벤트를 adb로도 보낼 수 있다(DemoEvent 참고).
 */
@Composable
fun DemoPanel(onEvent: (DemoEvent) -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(24.px)
    Column(
        modifier = modifier
            .padding(start = 96.px, bottom = 24.px)
            .width(560.px)
            .shadow(16.px, shape)
            .background(AutoColors.SurfaceRaised, shape)
            .border(2.px, AutoColors.Ink, shape)
            .padding(24.px),
        verticalArrangement = Arrangement.spacedBy(12.px),
    ) {
        Row(Modifier.fillMaxWidth()) {
            BasicText("데모 패널 (디버그)", style = PanelTitle.style(), modifier = Modifier.weight(1f))
            PillButton("닫기", onClose, heightPx = 52f, horizontalPaddingPx = 20f, textSpec = PanelButton)
        }
        DemoEvent.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.px)) {
                row.forEach { event ->
                    PillButton(
                        text = event.label,
                        onClick = { onEvent(event) },
                        modifier = Modifier.weight(1f),
                        enabled = event !in PendingEvents,
                        heightPx = 64f,
                        horizontalPaddingPx = 0f,
                        textSpec = PanelButton,
                    )
                }
                if (row.size == 1) Column(Modifier.weight(1f)) {}
            }
        }
    }
}
