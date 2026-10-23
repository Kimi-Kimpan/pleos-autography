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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.pleosautography.app.data.demo.ChapterStatus
import com.pleosautography.app.data.demo.ChapterSummary
import com.pleosautography.app.data.demo.DemoData
import com.pleosautography.app.data.demo.NowPlaying
import com.pleosautography.app.ui.components.CircleIconButton
import com.pleosautography.app.ui.components.HLine
import com.pleosautography.app.ui.components.PillButton
import com.pleosautography.app.ui.components.ProgressBar
import com.pleosautography.app.ui.components.RuledText
import com.pleosautography.app.ui.components.StatusChip
import com.pleosautography.app.ui.components.VLine
import com.pleosautography.app.ui.icons.AutoIcons
import com.pleosautography.app.ui.theme.AutoColors
import com.pleosautography.app.ui.theme.AutoType
import com.pleosautography.app.ui.theme.TextSpec
import com.pleosautography.app.ui.theme.px

/**
 * 01 홈 · 챕터 목록 (정차 중). 시안: design/screens/Main.png
 */
@Composable
fun MainScreen(
    onContinueWriting: () -> Unit,
    onListen: () -> Unit,
    onOpenScript: () -> Unit,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chapters = DemoData.chapters
    val writing = chapters.firstOrNull { it.status == ChapterStatus.WRITING }
    Column(modifier.fillMaxSize()) {
        Header(
            greeting = "안녕하세요 ${DemoData.OWNER_CALL_NAME}님!\n‘${writing?.title ?: chapters.first().title}’ 이야기를 이어서 써 볼까요?",
        )
        HLine(color = AutoColors.Ink)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            chapters.forEachIndexed { index, chapter ->
                if (chapter == writing) {
                    ExpandedChapter(chapter, onContinueWriting, onListen, onOpenScript)
                } else {
                    // 펼친 챕터와 맞닿은 구분선만 끝까지 긋고, 나머지는 안쪽으로 들인다.
                    val nextIsExpanded = chapters.getOrNull(index + 1) == writing
                    ChapterRow(
                        chapter = chapter,
                        fullWidthDivider = nextIsExpanded,
                        onAction = if (chapter.status == ChapterStatus.DONE) onListen else onContinueWriting,
                    )
                }
            }
        }
        HLine(color = AutoColors.Ink)
        MiniPlayer(DemoData.nowPlaying, onExpand = onOpenPlayer)
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
private fun ChapterRow(chapter: ChapterSummary, fullWidthDivider: Boolean, onAction: () -> Unit) {
    Column(Modifier.padding(horizontal = if (fullWidthDivider) 0.px else 32.px)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                PillButton("이어쓰기", onContinueWriting, Modifier.weight(1f), AutoIcons.Pencil, filled = true, horizontalPaddingPx = 0f)
                PillButton("들어보기", onListen, Modifier.weight(1f), AutoIcons.Play, iconSizePx = 26f, horizontalPaddingPx = 0f)
                PillButton("원고 보기", onOpenScript, Modifier.weight(1f), AutoIcons.Document, horizontalPaddingPx = 0f)
            }
        }
        HLine()
    }
}

@Composable
private fun MiniPlayer(nowPlaying: NowPlaying, onExpand: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 32.px, top = 26.px, end = 32.px, bottom = 32.px),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.px),
    ) {
        Box(
            modifier = Modifier
                .size(width = 84.px, height = 112.px)
                .background(AutoColors.CoverPaper)
                .border(1.5.px, AutoColors.Ink)
                .padding(9.5.px),
            contentAlignment = Alignment.BottomStart,
        ) {
            BasicText("Driving\nTogether", style = AutoType.CoverText.style())
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.px),
        ) {
            SingleLine(nowPlaying.label, AutoType.PlayerLabel, AutoColors.InkSecondary)
            SingleLine(nowPlaying.title, AutoType.PlayerTitle, AutoColors.Ink)
            SingleLine(nowPlaying.meta, AutoType.PlayerMeta, AutoColors.InkSecondary)
        }
        // 일시정지·다음은 주행 중에도 쓸 수 있는 컨트롤이다.
        CircleIconButton(AutoIcons.Pause, "일시정지", onClick = {}, sizePx = 92f, iconSizePx = 34f, borderPx = 3.5f)
        CircleIconButton(AutoIcons.Next, "다음 이야기", onClick = {})
        VLine(Modifier.height(64.px), color = AutoColors.Line)
        CircleIconButton(AutoIcons.ChevronUp, "플레이어 펼치기", onClick = onExpand, iconSizePx = 36f)
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

@Composable
private fun SingleLine(text: String, spec: TextSpec, color: Color, modifier: Modifier = Modifier) {
    BasicText(
        text = text,
        style = spec.style(color),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

private fun ChapterSummary.metaText(): String = when (status) {
    ChapterStatus.DONE -> "완료 · ${storyCount}편"
    ChapterStatus.WRITING -> "${storyCount}편 · 쓰는 중"
    ChapterStatus.NOT_STARTED -> "시작 전"
}
