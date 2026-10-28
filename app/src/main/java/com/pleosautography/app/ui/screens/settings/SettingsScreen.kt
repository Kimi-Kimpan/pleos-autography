package com.pleosautography.app.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import com.pleosautography.app.data.repo.BookSummary
import com.pleosautography.app.domain.interview.InterviewSession
import com.pleosautography.app.domain.listen.ListenSession
import com.pleosautography.app.ui.LocalAppServices
import com.pleosautography.app.ui.components.NoticeHost
import com.pleosautography.app.ui.components.rememberNoticeState
import com.pleosautography.app.ui.screens.main.TOTAL_CHAPTERS
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
import com.pleosautography.app.ui.components.CircleIconButton
import com.pleosautography.app.ui.components.HLine
import com.pleosautography.app.ui.components.PillButton
import com.pleosautography.app.ui.components.ScreenStatusChip
import com.pleosautography.app.ui.components.SingleLine
import com.pleosautography.app.ui.icons.AutoIcons
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

/**
 * 12 환경설정(정차 중). 시안: design/screens/Settings.png
 * 자서전 여러 권 관리(출판 신청·이 자서전 쓰기·이름 바꾸기·삭제·새로 시작)와 오토 설정. 모두 바로 저장한다.
 */
@Composable
fun SettingsScreen(
    onHome: () -> Unit,
    onNewBook: () -> Unit,
    modifier: Modifier = Modifier,
    showHome: Boolean = true,
) {
    val services = LocalAppServices.current
    val library = services?.library?.collectAsState()?.value
    val settings = library?.settings
    val repository = services?.repository
    val run: (suspend () -> Unit) -> Unit = { block -> services?.launch?.invoke(block) }
    val notice = rememberNoticeState()
    var renaming by rememberSaveable { mutableStateOf<Long?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingCallName by rememberSaveable { mutableStateOf(false) }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
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
                val active = library?.active
                // 지금 쓰는 책을 맨 위에, 나머지는 최근에 쓴 순서로.
                val books = library?.books.orEmpty().sortedWith(compareByDescending<BookSummary> { it.book.id == active?.id }.thenByDescending { it.lastActivityAt })
                books.forEach { summary ->
                    val book = summary.book
                    val current = book.id == active?.id
                    when {
                        renaming == book.id -> InlineEdit(
                            initial = book.title,
                            label = "자서전 제목",
                            onSave = { title ->
                                renaming = null
                                run { repository?.renameBook(book.id, title) }
                                notice.show("제목을 ‘$title’${ListenSession.directionParticle(title)} 바꿨어요")
                            },
                            onCancel = { renaming = null },
                        )
                        confirmDelete == book.id -> DeleteConfirm(
                            title = book.title,
                            stories = summary.stories,
                            onDelete = {
                                confirmDelete = null
                                run { repository?.deleteBook(book.id) }
                                notice.show("‘${book.title}’${InterviewSession.objectParticle(book.title)} 지웠어요")
                            },
                            onCancel = { confirmDelete = null },
                        )
                        else -> BookRowView(
                            summary = summary,
                            current = current,
                            onPrimary = {
                                if (current) {
                                    if (book.publishRequestedAt == null) run { repository?.requestPublish(book.id) }
                                    notice.show("출판 신청이 접수됐어요. 준비되면 알려 드릴게요")
                                } else {
                                    run { repository?.switchBook(book.id) }
                                    notice.show("이제 ‘${book.title}’${InterviewSession.objectParticle(book.title)} 써요")
                                }
                            },
                            onRename = { renaming = book.id; confirmDelete = null },
                            onDelete = { confirmDelete = book.id; renaming = null },
                        )
                    }
                    HLine()
                }
                NewBookButton(onNewBook)

                SectionLabel("오토", topPx = 34f)
                val callName = settings?.callName.orEmpty()
                if (editingCallName) {
                    InlineEdit(
                        initial = callName,
                        label = "부르는 이름",
                        onSave = { name ->
                            editingCallName = false
                            run { repository?.updateSettings { it.copy(callName = name) } }
                            notice.show("이제 ‘$name’${quoteParticle(name)} 불러 드릴게요")
                        },
                        onCancel = { editingCallName = false },
                    )
                    HLine()
                } else {
                    Option("부르는 이름", value = callName, onClick = { editingCallName = true })
                }
                val proactive = settings?.proactiveTalk ?: true
                Option(
                    "오토가 먼저 말 걸기",
                    value = if (proactive) "긴 직진·정체 15분" else "꺼짐",
                    checked = proactive,
                    onToggle = { on -> run { repository?.updateSettings { it.copy(proactiveTalk = on) } } },
                )
                Option(
                    "알림 멜로디",
                    checked = settings?.chimeOn ?: true,
                    onToggle = { on -> run { repository?.updateSettings { it.copy(chimeOn = on) } } },
                )
                val public = settings?.radioPublic ?: false
                Option(
                    "오토그래피 라디오에 공개",
                    value = if (public) "공개 중" else "나중에",
                    checked = public,
                    onToggle = { on -> run { repository?.updateSettings { it.copy(radioPublic = on) } } },
                )

                // 시연 모드: 심사·시연용. 샘플 자서전과 데모 패널(책등 ‘Autography’ 길게 누르기)을 쓴다. 환경설정은 정차 중에만 열린다.
                SectionLabel("시연", topPx = 34f)
                val demoMode = settings?.demoMode ?: false
                Option(
                    "시연 모드",
                    value = if (demoMode) "데모 값 사용 중" else "꺼짐",
                    checked = demoMode,
                    onToggle = { on ->
                        services?.setDemoMode?.invoke(on)
                        notice.show(if (on) "시연 모드를 켰어요. 책등의 ‘Autography’를 길게 누르면 데모 패널이 열려요" else "시연 모드를 껐어요. 실제 차량 값으로 돌아갔어요")
                    },
                    last = !demoMode,
                )
                if (demoMode) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 18.px, bottom = 32.px),
                        verticalArrangement = Arrangement.spacedBy(14.px),
                    ) {
                        PillButton(
                            text = "샘플 자서전 불러오기",
                            onClick = {
                                run {
                                    val created = services?.loadSampleBook?.invoke() ?: false
                                    notice.show(if (created) "샘플 자서전을 불러왔어요 · ‘첫 차’ 71%" else "샘플 자서전으로 바꿨어요")
                                }
                            },
                            icon = AutoIcons.Book,
                            heightPx = 84f,
                            iconSizePx = 28f,
                            textSpec = Action,
                        )
                        BasicText(
                            "책등의 ‘Autography’를 길게 누르면 데모 패널이 열려요(주행·정차, “오토야, 뭐해?” 등). 시연 모드를 꺼도 불러온 책은 남아요.",
                            style = BookMeta.style(AutoColors.InkSecondary),
                        )
                    }
                }
            }
        }
        NoticeHost(notice, Modifier.align(Alignment.BottomCenter))
    }
}

/** 자서전 한 줄: 제목·진행, 출판 신청(지금 쓰는 책) 또는 이 자서전 쓰기, 이름 바꾸기, 삭제. */
@Composable
private fun BookRowView(summary: BookSummary, current: Boolean, onPrimary: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    val book = summary.book
    val published = book.publishRequestedAt != null
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 22.px),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.px),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.px)) {
            SingleLine(book.title, BookTitle, AutoColors.Ink)
            val meta = when {
                current && published -> "출판 신청 접수됨"
                current -> "지금 쓰는 중"
                else -> relativeTime(summary.lastActivityAt)
            }
            SingleLine("${summary.doneChapters} / $TOTAL_CHAPTERS 챕터 · $meta", BookMeta, AutoColors.InkSecondary)
        }
        PillButton(
            text = when {
                !current -> "이 자서전 쓰기"
                published -> "신청 접수됨"
                else -> "출판 신청"
            },
            onClick = onPrimary,
            icon = if (current) AutoIcons.Book else null,
            heightPx = 68f,
            iconSizePx = 24f,
            horizontalPaddingPx = 22f,
            gapPx = 8f,
            textSpec = Action,
        )
        CircleIconButton(AutoIcons.Pencil, "${book.title} 제목 바꾸기", onClick = onRename, sizePx = 68f, iconSizePx = 26f)
        CircleIconButton(AutoIcons.Trash, "${book.title} 삭제", onClick = onDelete, sizePx = 68f, iconSizePx = 26f)
    }
}

/** 지우기 전 확인. 답변이 함께 지워진다는 것을 알린다. */
@Composable
private fun DeleteConfirm(title: String, stories: Int, onDelete: () -> Unit, onCancel: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 22.px),
        verticalArrangement = Arrangement.spacedBy(16.px),
    ) {
        BasicText("‘$title’${InterviewSession.objectParticle(title)} 지울까요?", style = BookTitle.style())
        BasicText(
            if (stories > 0) "이야기 ${stories}편과 원고가 모두 지워지고, 되돌릴 수 없어요." else "되돌릴 수 없어요.",
            style = BookMeta.style(AutoColors.InkSecondary),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(14.px)) {
            PillButton("지우기", onDelete, icon = AutoIcons.Trash, filled = true, heightPx = 68f, iconSizePx = 24f, horizontalPaddingPx = 26f, textSpec = Action)
            PillButton("취소", onCancel, heightPx = 68f, horizontalPaddingPx = 26f, textSpec = Action)
        }
    }
}

/** 한 줄 고치기(제목·부르는 이름): 알약 입력칸 + 저장·취소. 빈칸이면 저장하지 않는다. */
@Composable
private fun InlineEdit(initial: String, label: String, onSave: (String) -> Unit, onCancel: () -> Unit) {
    var value by rememberSaveable { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.px),
        verticalArrangement = Arrangement.spacedBy(14.px),
    ) {
        BasicText(label, style = BookMeta.style(AutoColors.InkSecondary))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.px)) {
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                textStyle = BookTitle.style(),
                cursorBrush = SolidColor(AutoColors.Ink),
                modifier = Modifier
                    .weight(1f)
                    .height(76.px)
                    .border(2.5.px, AutoColors.Ink, RoundedCornerShape(percent = 50))
                    .padding(horizontal = 30.px)
                    .wrapContentHeight(Alignment.CenterVertically)
                    .focusRequester(focus),
            )
            PillButton("저장", { value.trim().takeIf { it.isNotEmpty() }?.let(onSave) }, filled = true, enabled = value.isNotBlank(), heightPx = 68f, horizontalPaddingPx = 26f, textSpec = Action)
            PillButton("취소", onCancel, heightPx = 68f, horizontalPaddingPx = 26f, textSpec = Action)
        }
    }
}

/** 마지막으로 쓴 때(“오늘”, “3일 전”, “3주 전”). */
private fun relativeTime(at: Long): String {
    val days = ((System.currentTimeMillis() - at) / DAY_MS).toInt()
    return when {
        days <= 0 -> "오늘"
        days < 7 -> "${days}일 전"
        days < 30 -> "${days / 7}주 전"
        else -> "${days / 30}달 전"
    }
}

private const val DAY_MS = 24 * 60 * 60 * 1000L

/** “이라고/라고”: 받침이 있으면 “이라고”. */
private fun quoteParticle(word: String): String {
    val last = word.lastOrNull { it in '가'..'힣' } ?: return "라고"
    return if ((last - '가') % 28 != 0) "이라고" else "라고"
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
    onClick: (() -> Unit)? = null,
    last: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 92.px)
            .then(
                when {
                    checked != null -> Modifier.clickable(role = Role.Switch) { onToggle(!checked) }
                    onClick != null -> Modifier.clickable(role = Role.Button, onClickLabel = "$name 바꾸기", onClick = onClick)
                    else -> Modifier
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.px),
    ) {
        BasicText(name, style = OptionName.style(), modifier = Modifier.weight(1f))
        if (value != null) BasicText(value, style = OptionValue.style(AutoColors.InkSecondary))
        if (checked != null) Toggle(checked, name)
        if (onClick != null) Image(rememberVectorPainter(AutoIcons.Pencil), null, Modifier.size(26.px), colorFilter = ColorFilter.tint(AutoColors.InkSecondary))
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
