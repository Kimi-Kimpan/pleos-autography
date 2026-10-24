package com.pleosautography.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.pleosautography.app.ui.theme.AutoColors
import com.pleosautography.app.ui.theme.TextSpec
import com.pleosautography.app.ui.theme.px
import kotlinx.coroutines.delay

private val NoticeText = TextSpec(26f, FontWeight.Bold)
private val NoticeAction = TextSpec(26f, FontWeight.ExtraBold)

/** 잠시 떴다 사라지는 안내 한 줄. [actionLabel]이 있으면 오른쪽에 버튼(예: 되돌리기)을 둔다. */
data class Notice(
    val text: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
    val id: Long = System.nanoTime(),
)

@Stable
class NoticeState {
    var current by mutableStateOf<Notice?>(null)

    fun show(text: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        current = Notice(text, actionLabel, onAction)
    }
}

@Composable
fun rememberNoticeState(): NoticeState = remember { NoticeState() }

/** 화면 아래 가운데에 잉크색 알약으로 안내를 띄우고, 4초 뒤 닫는다. */
@Composable
fun NoticeHost(state: NoticeState, modifier: Modifier = Modifier) {
    val notice = state.current ?: return
    LaunchedEffect(notice.id) {
        delay(NOTICE_MS)
        if (state.current?.id == notice.id) state.current = null
    }
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            .padding(bottom = 40.px)
            .shadow(12.px, shape)
            .clip(shape)
            .background(AutoColors.Ink)
            .heightIn(min = 84.px)
            .padding(horizontal = 36.px)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(28.px),
    ) {
        BasicText(notice.text, style = NoticeText.style(AutoColors.OnInk))
        if (notice.actionLabel != null && notice.onAction != null) {
            BasicText(
                notice.actionLabel,
                style = NoticeAction.style(AutoColors.LavenderSoft),
                modifier = Modifier
                    .clickable(role = Role.Button) {
                        notice.onAction.invoke()
                        state.current = null
                    }
                    .padding(vertical = 18.px),
            )
        }
    }
}

private const val NOTICE_MS = 4_000L
