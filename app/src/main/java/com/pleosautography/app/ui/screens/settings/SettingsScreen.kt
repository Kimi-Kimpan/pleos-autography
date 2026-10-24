package com.pleosautography.app.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import com.pleosautography.app.data.demo.DemoData
import com.pleosautography.app.ui.components.CircleIconButton
import com.pleosautography.app.ui.components.HLine
import com.pleosautography.app.ui.components.PillButton
import com.pleosautography.app.ui.components.ScreenStatusChip
import com.pleosautography.app.ui.components.SingleLine
import com.pleosautography.app.ui.icons.AutoIcons
import com.pleosautography.app.ui.screens.LocalBookState
import com.pleosautography.app.ui.theme.AutoColors
import com.pleosautography.app.ui.theme.TextSpec
import com.pleosautography.app.ui.theme.px

private val Title = TextSpec(52f, FontWeight.ExtraBold)
private val Section = TextSpec(24f, FontWeight.ExtraBold, letterSpacingEm = 0.04f)
private val BookTitle = TextSpec(30f, FontWeight.ExtraBold)
private val BookMeta = TextSpec(22f, FontWeight.SemiBold)
private val Action = TextSpec(24f, FontWeight.ExtraBold)
private val OptionName = TextSpec(28f, FontWeight.Bold)
private val OptionValue = TextSpec(24f, FontWeight.SemiBold)

/** 자서전 한 권(설정 목록). */
private data class BookRow(val title: String, val meta: String, val current: Boolean)

/**
 * 12 환경설정(정차 중). 시안: design/screens/Settings.png
 * 자서전 여러 권 관리(출판 신청·제목 바꾸기·삭제·새로 시작)와 오토 설정.
 * 설정 값은 지금 화면 안에서만 바뀌고, 저장은 Settings 테이블을 붙일 때 연결한다.
 */
@Composable
fun SettingsScreen(
    onHome: () -> Unit,
    onPublish: () -> Unit,
    onNewBook: () -> Unit,
    modifier: Modifier = Modifier,
    showHome: Boolean = true,
) {
    val done = LocalBookState.current.stats?.doneChapters ?: DemoData.book.doneChapters
    val books = listOf(
        BookRow(DemoData.BOOK_TITLE, "$done / 12 챕터 · 지금 쓰는 중", current = true),
        BookRow("우리 가게 이야기", "1 / 12 챕터 · 3주 전", current = false),
    )
    var proactive by rememberSaveable { mutableStateOf(true) }
    var chime by rememberSaveable { mutableStateOf(true) }
    var radioPublic by rememberSaveable { mutableStateOf(false) }

    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.px, top = 32.px, end = 32.px, bottom = 26.px),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.px),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.px)) {
                ScreenStatusChip("정차 중 · P", filled = false)
                BasicText("환경설정", style = Title.style())
            }
            if (showHome) {
                PillButton(
                    text = "홈",
                    onClick = onHome,
                    icon = AutoIcons.ChevronLeft,
                    heightPx = 100f,
                    iconSizePx = 34f,
                    startPaddingPx = 26f,
                    endPaddingPx = 36f,
                    gapPx = 10f,
                    textSpec = TextSpec(34f, FontWeight.ExtraBold),
                )
            }
        }
        HLine(color = AutoColors.Ink)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.px),
        ) {
            SectionLabel("내 자서전", topPx = 26f)
            books.forEach { book ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 22.px),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.px),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.px)) {
                        SingleLine(book.title, BookTitle, AutoColors.Ink)
                        SingleLine(book.meta, BookMeta, AutoColors.InkSecondary)
                    }
                    PillButton(
                        text = if (book.current) "출판 신청" else "이 자서전 쓰기",
                        onClick = if (book.current) onPublish else ({}),
                        icon = if (book.current) AutoIcons.Book else null,
                        heightPx = 68f,
                        iconSizePx = 24f,
                        horizontalPaddingPx = 22f,
                        gapPx = 8f,
                        textSpec = Action,
                    )
                    CircleIconButton(AutoIcons.Pencil, "${book.title} 제목 바꾸기", onClick = {}, sizePx = 68f, iconSizePx = 26f)
                    CircleIconButton(AutoIcons.Trash, "${book.title} 삭제", onClick = {}, sizePx = 68f, iconSizePx = 26f)
                }
                HLine()
            }
            NewBookButton(onNewBook)

            SectionLabel("오토", topPx = 34f)
            Option("부르는 이름", value = "${DemoData.OWNER_CALL_NAME}님")
            Option("오토가 먼저 말 걸기", value = "긴 직진·정체 15분", checked = proactive, onToggle = { proactive = it })
            Option("알림 멜로디", checked = chime, onToggle = { chime = it })
            Option("오토그래피 라디오에 공개", value = "나중에", checked = radioPublic, onToggle = { radioPublic = it }, last = true)
        }
    }
}

@Composable
private fun SectionLabel(text: String, topPx: Float) {
    BasicText(text, style = Section.style(AutoColors.InkSecondary), modifier = Modifier.padding(top = topPx.px, bottom = 10.px))
}

/** 점선 테두리 알약: 새로운 자서전 시작하기(첫 실행 화면으로). */
@Composable
private fun NewBookButton(onClick: () -> Unit) {
    val border = 2.5.px
    Row(
        modifier = Modifier
            .padding(top = 18.px)
            .fillMaxWidth()
            .height(96.px)
            .clip(RoundedCornerShape(percent = 50))
            .drawBehind {
                val w = border.toPx()
                drawRoundRect(
                    color = AutoColors.Ink,
                    topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2),
                    size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
                    cornerRadius = CornerRadius((size.height - w) / 2),
                    style = Stroke(width = w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * 3, w * 3))),
                )
            }
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.px, Alignment.CenterHorizontally),
    ) {
        Image(rememberVectorPainter(AutoIcons.Plus), null, Modifier.size(32.px), colorFilter = ColorFilter.tint(AutoColors.Ink))
        BasicText("새로운 자서전 시작하기", style = TextSpec(30f, FontWeight.ExtraBold).style())
    }
}

/** 설정 한 줄. [checked]가 있으면 오른쪽에 켜고 끄는 스위치를 둔다. */
@Composable
private fun Option(
    name: String,
    value: String? = null,
    checked: Boolean? = null,
    onToggle: (Boolean) -> Unit = {},
    last: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 92.px)
            .then(if (checked != null) Modifier.clickable(role = Role.Switch) { onToggle(!checked) } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.px),
    ) {
        BasicText(name, style = OptionName.style(), modifier = Modifier.weight(1f))
        if (value != null) BasicText(value, style = OptionValue.style(AutoColors.InkSecondary))
        if (checked != null) Toggle(checked, name)
    }
    if (!last) HLine()
}

/** 84×48 스위치. 켜짐은 잉크, 꺼짐은 회색 바탕에 흰 손잡이(36). */
@Composable
private fun Toggle(checked: Boolean, label: String) {
    Box(
        modifier = Modifier
            .size(width = 84.px, height = 48.px)
            .clip(CircleShape)
            .background(if (checked) AutoColors.Ink else AutoColors.Line)
            .padding(6.px)
            .semantics {
                contentDescription = label
                stateDescription = if (checked) "켜짐" else "꺼짐"
            },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(Modifier.size(36.px).clip(CircleShape).background(AutoColors.OnInk))
    }
}
