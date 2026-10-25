package com.pleosautography.app.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.pleosautography.app.data.demo.ChapterStatus
import com.pleosautography.app.data.demo.ChapterSummary
import com.pleosautography.app.data.demo.DemoData
import com.pleosautography.app.ui.screens.LocalBookState
import com.pleosautography.app.ui.components.CircleIconButton
import com.pleosautography.app.ui.components.HLine
import com.pleosautography.app.ui.components.MiniPlayer
import com.pleosautography.app.ui.components.SingleLine
import com.pleosautography.app.ui.components.PillButton
import com.pleosautography.app.ui.components.ProgressBar
import com.pleosautography.app.ui.components.RuledText
import com.pleosautography.app.ui.components.StatusChip
import com.pleosautography.app.ui.components.VLine
import com.pleosautography.app.ui.icons.AutoIcons
import com.pleosautography.app.ui.screens.currentChapter
import com.pleosautography.app.ui.screens.live
import com.pleosautography.app.ui.theme.AutoColors
import com.pleosautography.app.ui.theme.AutoType
import com.pleosautography.app.ui.theme.TextSpec
import com.pleosautography.app.ui.theme.px

/** 펼친 챕터의 버튼. 가로 전체 창에서 오른쪽에 열린 화면을 표시할 때 쓴다. */
enum class ChapterAction { CONTINUE, LISTEN, SCRIPT }

/**
 * 01 홈 · 챕터 목록 (정차 중). 시안: design/screens/Main.png
 * 가로 전체 창에서는 인사말 머리글 없이 목록만 쓰고(인사말은 오른쪽 칸으로), 시안 FullPark.
 *
 * @param selectedAction 오른쪽 칸에 열린 화면의 버튼(선택 표시).
 * @param radioPaused 정차 중 인터뷰가 열려 라디오를 잠시 멈췄는지.
 * @param onStartChapter 시작 전 챕터의 ＋: 그 챕터를 지금 쓰는 챕터로 바꾸고 인터뷰를 연다.
 * @param onOpenChapterScript 완료한 챕터 줄을 누르면 그 챕터 원고를 연다.
 * @param onStopHere 가로 전체 창에서 오른쪽에 인터뷰가 열려 있으면 ‘이어쓰기’ 자리가 파란 ‘여기까지’가 되고, 누르면 이것을 부른다.
 */
@Composable
fun MainScreen(
    onContinueWriting: () -> Unit,
    onListen: () -> Unit,
    onOpenScript: () -> Unit,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
    onStartChapter: (Int) -> Unit = { onContinueWriting() },
    onOpenChapterScript: (Int) -> Unit = { onOpenScript() },
    onStopHere: () -> Unit = {},
    showHeader: Boolean = true,
    selectedAction: ChapterAction? = null,
    radioPaused: Boolean = false,
) {
    val chapters = DemoData.chapters.map { it.live() }
    val writing = chapters.firstOrNull { it.status == ChapterStatus.WRITING }
    Column(modifier.fillMaxSize()) {
        if (showHeader) {
            Header(greeting = parkedGreeting(singleLine = true))
            HLine(color = AutoColors.Ink)
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            chapters.forEachIndexed { index, chapter ->
                if (chapter == writing) {
                    ExpandedChapter(chapter, onContinueWriting, onListen, onOpenScript, selectedAction, onStopHere)
                } else {
                    // 펼친 챕터와 맞닿은 구분선만 끝까지 긋고, 나머지는 안쪽으로 들인다.
                    val nextIsExpanded = chapters.getOrNull(index + 1) == writing
                    ChapterRow(
                        chapter = chapter,
                        fullWidthDivider = nextIsExpanded,
                        onAction = if (chapter.status == ChapterStatus.DONE) onListen else ({ onStartChapter(chapter.number) }),
                        onOpen = if (chapter.status == ChapterStatus.DONE) ({ onOpenChapterScript(chapter.number) }) else null,
                    )
                }
            }
        }
        MiniPlayer(onExpand = onOpenPlayer, pausedForInterview = radioPaused)
    }
}

@Composable
private fun Header(greeting: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 32.px, top = 32.px, end = 32.px, bottom = 54.px),
        verticalArrangement = Arrangement.spacedBy(12.px),
    ) {
        StatusChip("정차 중 · P")
        RuledText(
            text = greeting,
            spec = AutoType.GreetingParked,
            modifier = Modifier
                .widthIn(max = 748.px)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun ChapterRow(chapter: ChapterSummary, fullWidthDivider: Boolean, onAction: () -> Unit, onOpen: (() -> Unit)? = null) {
    Column(Modifier.padding(horizontal = if (fullWidthDivider) 0.px else 32.px)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onOpen != null) Modifier.clickable(role = Role.Button, onClickLabel = "${chapter.number}장 원고 보기", onClick = onOpen) else Modifier)
                .height(138.px)
                .padding(horizontal = if (fullWidthDivider) 32.px else 0.px),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.px),
        ) {
            ChapterNumber(chapter.number, AutoColors.InkSecondary)
            SingleLine(chapter.title, AutoType.ChapterTitle, AutoColors.Ink, Modifier.weight(1f))
            SingleLine(chapter.metaText(), AutoType.Meta, AutoColors.InkSecondary)
            CircleIconButton(
                icon = if (chapter.status == ChapterStatus.DONE) AutoIcons.Play else AutoIcons.Plus,
                contentDescription = if (chapter.status == ChapterStatus.DONE) "${chapter.number}장 들어보기" else "${chapter.number}장 시작하기",
                onClick = onAction,
            )
        }
        HLine()
    }
}

@Composable
private fun ExpandedChapter(
    chapter: ChapterSummary,
    onContinueWriting: () -> Unit,
    onListen: () -> Unit,
    onOpenScript: () -> Unit,
    selectedAction: ChapterAction?,
    onStopHere: () -> Unit,
) {
    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AutoColors.SurfaceRaised)
                .padding(start = 32.px, top = 30.px, end = 32.px, bottom = 34.px),
            verticalArrangement = Arrangement.spacedBy(24.px),
        ) {
            // 시안의 줄 높이(1.1em)가 Asta Sans 글자 높이(1.19em)보다 작아서, CSS처럼 줄 상자 높이를 고정한다.
            Row(
                modifier = Modifier.height(57.2.px),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.px),
            ) {
                ChapterNumber(chapter.number, AutoColors.Ink)
                SingleLine(chapter.title, AutoType.ChapterTitleExpanded, AutoColors.Ink, Modifier.weight(1f))
                SingleLine(chapter.metaText(), AutoType.Meta, AutoColors.InkSecondary)
            }
            Column(
                modifier = Modifier.padding(start = 88.px),
                verticalArrangement = Arrangement.spacedBy(10.px),
            ) {
                ProgressBar(chapter.progress)
                chapter.lastStory?.let {
                    SingleLine("마지막 이야기: $it", AutoType.LastStory, AutoColors.InkSecondary)
                }
            }
            // 창 폭이 시안보다 좁아도 버튼 세 개가 한 줄에 들어가도록 폭을 나눠 쓴다.
            Row(
                modifier = Modifier.padding(start = 88.px),
                horizontalArrangement = Arrangement.spacedBy(16.px),
            ) {
                if (selectedAction == ChapterAction.CONTINUE) {
                    // 오른쪽에서 인터뷰 중: 지금 말하기를 끝내는 ‘여기까지’로 바꾼다.
                    PillButton("여기까지", onStopHere, Modifier.weight(1f), AutoIcons.Clip, filled = true, horizontalPaddingPx = 0f)
                } else {
                    PillButton("이어쓰기", onContinueWriting, Modifier.weight(1f), AutoIcons.Pencil, filled = true, horizontalPaddingPx = 0f)
                }
                PillButton("들어보기", onListen, Modifier.weight(1f), AutoIcons.Play, iconSizePx = 26f, horizontalPaddingPx = 0f, selected = selectedAction == ChapterAction.LISTEN)
                PillButton("원고 보기", onOpenScript, Modifier.weight(1f), AutoIcons.Document, horizontalPaddingPx = 0f, selected = selectedAction == ChapterAction.SCRIPT)
            }
        }
        HLine()
    }
}

@Composable
private fun ChapterNumber(number: Int, color: Color) {
    BasicText(
        text = "%02d".format(number),
        style = AutoType.ChapterNumber.style(color),
        modifier = Modifier.width(64.px),
    )
}

private fun ChapterSummary.metaText(): String = when (status) {
    ChapterStatus.DONE -> "완료 · ${storyCount}편"
    ChapterStatus.WRITING -> "${storyCount}편 · 쓰는 중"
    ChapterStatus.NOT_STARTED -> "시작 전"
}

/** 정차 중 인사말. 세로 창 머리글은 두 줄, 가로 전체 창 오른쪽 칸은 세 줄로 쓴다. */
@Composable
@ReadOnlyComposable
fun parkedGreeting(singleLine: Boolean): String {
    val title = currentChapter().title
    val story = if (singleLine) "‘$title’ 이야기를 이어서 써 볼까요?" else "‘$title’ 이야기를\n이어서 써 볼까요?"
    return "안녕하세요${LocalBookState.current.greetingName}!\n$story"
}
