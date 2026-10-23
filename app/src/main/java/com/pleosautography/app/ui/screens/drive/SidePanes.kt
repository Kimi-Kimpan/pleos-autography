package com.pleosautography.app.ui.screens.drive

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.pleosautography.app.data.demo.ChapterStatus
import com.pleosautography.app.data.demo.ChapterSummary
import com.pleosautography.app.data.demo.DemoData
import com.pleosautography.app.data.demo.RadioEpisode
import com.pleosautography.app.ui.components.CircleIconButton
import com.pleosautography.app.ui.components.HLine
import com.pleosautography.app.ui.components.ProgressBar
import com.pleosautography.app.ui.components.SingleLine
import com.pleosautography.app.ui.components.StatusChip
import com.pleosautography.app.ui.icons.AutoIcons
import com.pleosautography.app.ui.icons.Illustrations
import com.pleosautography.app.ui.theme.AutoColors
import com.pleosautography.app.ui.theme.DisabledAlpha
import com.pleosautography.app.ui.theme.GowunBatang
import com.pleosautography.app.ui.theme.TextSpec
import com.pleosautography.app.ui.theme.px
import kotlin.math.roundToInt

/*
 * 가로 전체 창 주행 중 오른쪽 칸(듣기·보기 전용). 시안: design/screens/FullDrive.png, FullInterview.png
 * 마이크를 쓰는 화면은 왼쪽 하나뿐이고, 오른쪽에는 말로 조작할 것이 없다. 스크롤도 없다.
 */

private val PaneLabel = TextSpec(18f, FontWeight.ExtraBold, letterSpacingEm = 0.12f)
private val InfoLabel = TextSpec(21f, FontWeight.Bold)
private val InfoMeta = TextSpec(22f, FontWeight.SemiBold)
private val Footnote = TextSpec(20f, FontWeight.SemiBold)

private val CoverInk = Color(0xFF111214)
private val CoverArt = Color(0xFFB9B6A9)
private val CardShadow = Color(0x29111214)

/** 칸 이름. 오른쪽 위에는 창 전체의 상태 칩이 겹쳐 그려지므로 칩은 두지 않는다. */
@Composable
private fun PaneHeader(label: String) {
    Box(Modifier.fillMaxWidth().height(36.px), contentAlignment = Alignment.CenterStart) {
        BasicText(label, style = PaneLabel.style())
    }
}

/** 주행 홈 오른쪽: 오토그래피 라디오. 재생 조작만 쓰고, 검색·장르는 주행 중 비활성이다. */
@Composable
fun RadioSidePane(modifier: Modifier = Modifier) {
    val radio = DemoData.radio
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 40.px, top = 36.px, end = 40.px, bottom = 40.px),
    ) {
        PaneHeader("AUTOGRAPHY RADIO")
        Row(
            modifier = Modifier.padding(top = 30.px),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(32.px),
        ) {
            RadioCover(radio.coverVolume)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.px)) {
                SingleLine(radio.nowPlayingLabel, InfoLabel, AutoColors.InkSecondary)
                SingleLine(radio.title, TextSpec(40f, FontWeight.ExtraBold, lineHeightPx = 46f), AutoColors.Ink)
                SingleLine(radio.narrator, InfoMeta, AutoColors.InkSecondary)
                SeekBar(radio.position, Modifier.padding(top = 8.px))
                Row(Modifier.fillMaxWidth()) {
                    val time = TextSpec(19f, FontWeight.SemiBold, tabularNumbers = true)
                    BasicText(radio.elapsed, style = time.style(AutoColors.InkSecondary), modifier = Modifier.weight(1f))
                    BasicText(radio.duration, style = time.style(AutoColors.InkSecondary))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.px)) {
                    RewindButton()
                    CircleIconButton(AutoIcons.Pause, "일시정지", onClick = {}, sizePx = 104f, iconSizePx = 40f, borderPx = 4f)
                    CircleIconButton(AutoIcons.Next, "다음 이야기", onClick = {})
                }
            }
        }
        Column(Modifier.padding(top = 34.px)) {
            HLine(color = AutoColors.Ink)
            Column(
                modifier = Modifier.padding(top = 28.px),
                verticalArrangement = Arrangement.spacedBy(16.px),
            ) {
                // 검색과 장르 칩은 주행 중에 쓰지 않는다.
                Column(Modifier.alpha(DisabledAlpha), verticalArrangement = Arrangement.spacedBy(14.px)) {
                    SearchField()
                    Row(horizontalArrangement = Arrangement.spacedBy(10.px)) {
                        radio.genres.forEachIndexed { index, genre -> GenreChip(genre, selected = index == 0) }
                    }
                }
                BasicText("검색과 목록 탐색은 정차 중에 열려요", style = Footnote.style(AutoColors.InkMuted))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 26.px), verticalAlignment = Alignment.Bottom) {
            BasicText("다음으로 들을 이야기", style = TextSpec(28f, FontWeight.ExtraBold).style(), modifier = Modifier.weight(1f))
            BasicText("${DemoData.OWNER_CALL_NAME}님과 비슷한 이야기", style = Footnote.style(AutoColors.InkSecondary))
        }
        Column(Modifier.padding(top = 6.px)) {
            radio.upNext.forEachIndexed { index, episode ->
                EpisodeRow(episode)
                if (index < radio.upNext.lastIndex) HLine()
            }
        }
    }
}

@Composable
private fun RadioCover(volume: String) {
    Column(
        modifier = Modifier
            .size(width = 228.px, height = 304.px)
            .shadow(14.px, RectangleShape, ambientColor = CardShadow, spotColor = CardShadow)
            .background(AutoColors.CoverPaper)
            .border(2.px, AutoColors.Ink)
            .padding(16.px),
        verticalArrangement = Arrangement.spacedBy(10.px),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(CoverArt)
                .padding(10.px),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Image(rememberVectorPainter(Illustrations.Truck), contentDescription = null, modifier = Modifier.fillMaxWidth())
        }
        BasicText("Driving Together", style = TextSpec(23f, FontWeight.ExtraBold, lineHeightPx = 24f).style(CoverInk), maxLines = 1)
        BasicText(volume, style = TextSpec(11f, FontWeight.SemiBold).style(CoverInk))
    }
}

@Composable
private fun SeekBar(position: Float, modifier: Modifier = Modifier) {
    // 시안처럼 막대 높이(12px)만 자리를 차지하고, 손잡이(26px)는 위아래로 넘친다.
    Box(modifier.fillMaxWidth().height(12.px), contentAlignment = Alignment.CenterStart) {
        ProgressBar(position)
        Row(Modifier.fillMaxWidth().wrapContentHeight(unbounded = true), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(position.coerceAtLeast(0.001f)))
            Box(Modifier.offset(x = (-13).px).size(26.px).clip(CircleShape).background(AutoColors.Ink))
            Spacer(Modifier.weight((1f - position).coerceAtLeast(0.001f)))
        }
    }
}

@Composable
private fun SearchField() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.px)
            .border(2.5.px, AutoColors.Ink, RoundedCornerShape(percent = 50))
            .padding(horizontal = 26.px),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.px),
    ) {
        Image(rememberVectorPainter(AutoIcons.Search), null, Modifier.size(28.px), colorFilter = ColorFilter.tint(AutoColors.Ink))
        BasicText("이야기, 기사님, 장소 검색", style = TextSpec(23f, FontWeight.SemiBold).style())
    }
}

@Composable
private fun GenreChip(text: String, selected: Boolean) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = Modifier
            .height(56.px)
            .clip(shape)
            .then(if (selected) Modifier.background(AutoColors.Ink) else Modifier.border(2.px, AutoColors.Ink, shape))
            .padding(horizontal = 22.px),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = TextSpec(21f, FontWeight.Bold).style(if (selected) AutoColors.OnInk else AutoColors.Ink), maxLines = 1)
    }
}

@Composable
private fun EpisodeRow(episode: RadioEpisode) {
    Row(
        modifier = Modifier.fillMaxWidth().height(106.px),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.px),
    ) {
        Box(
            modifier = Modifier
                .size(width = 60.px, height = 80.px)
                .background(AutoColors.CoverPaper)
                .border(1.5.px, AutoColors.Ink)
                .padding(6.5.px),
            contentAlignment = Alignment.BottomStart,
        ) {
            BasicText("Driving\nTogether", style = TextSpec(9f, FontWeight.ExtraBold, lineHeightPx = 9.9f).style())
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.px)) {
            SingleLine(episode.title, TextSpec(27f, FontWeight.Bold), AutoColors.Ink)
            SingleLine(episode.narrator, TextSpec(19f, FontWeight.Medium), AutoColors.InkSecondary)
        }
        StatusChip(episode.tag, sizePx = 18f, horizontalPaddingPx = 12f, verticalPaddingPx = 3f)
        CircleIconButton(AutoIcons.Play, "${episode.title} 재생", onClick = {}, sizePx = 72f, iconSizePx = 28f)
    }
}

/** 인터뷰 중 오른쪽: 내 자서전. 라디오는 멈춰 있고, 목록의 듣기·시작 버튼은 비활성이다. */
@Composable
fun MyBookSidePane(modifier: Modifier = Modifier) {
    val book = DemoData.book
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 36.px, bottom = 40.px),
    ) {
        Column(Modifier.padding(horizontal = 40.px)) {
            PaneHeader("MY AUTOGRAPHY")
            Row(
                modifier = Modifier.padding(top = 30.px),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(32.px),
            ) {
                BookCard(book.doneChapters, book.totalChapters)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.px)) {
                    SingleLine("지금까지 쓴 자서전", InfoLabel, AutoColors.InkSecondary)
                    SingleLine(DemoData.BOOK_TITLE, TextSpec(42f, FontWeight.Bold, GowunBatang, lineHeightPx = 50.4f), AutoColors.Ink)
                    SingleLine("이야기 ${book.storyCount}편 · 오토와 나눈 대화 ${book.conversationCount}회", InfoMeta, AutoColors.InkSecondary)
                    ProgressBar(book.progress, Modifier.padding(top = 8.px))
                    BasicText(
                        "전체 ${book.totalChapters}챕터 중 ${(book.progress * 100).roundToInt()}%",
                        style = TextSpec(19f, FontWeight.SemiBold).style(AutoColors.InkSecondary),
                    )
                }
            }
            HLine(Modifier.padding(top = 34.px), color = AutoColors.Ink)
        }
        DemoData.chapters.forEach { chapter ->
            if (chapter.status == ChapterStatus.WRITING) CurrentChapterRow(chapter) else BookChapterRow(chapter)
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.padding(horizontal = 40.px),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.px),
        ) {
            Image(rememberVectorPainter(AutoIcons.Pause), null, Modifier.size(18.px), colorFilter = ColorFilter.tint(AutoColors.InkMuted))
            BasicText(
                "라디오는 인터뷰 중 잠시 멈춰요 · 07~12장과 원고 보기·듣기는 정차 중에 열려요",
                style = Footnote.style(AutoColors.InkMuted),
            )
        }
    }
}

@Composable
private fun BookCard(done: Int, total: Int) {
    Column(
        modifier = Modifier
            .size(width = 180.px, height = 240.px)
            .shadow(14.px, RectangleShape, ambientColor = CardShadow, spotColor = CardShadow)
            .background(AutoColors.Bg)
            .border(2.px, AutoColors.Ink)
            .padding(start = 18.px, top = 20.px, end = 18.px, bottom = 20.px),
    ) {
        Box(
            Modifier
                .border(1.5.px, AutoColors.Ink, RoundedCornerShape(percent = 50))
                .padding(horizontal = 9.5.px, vertical = 2.5.px),
        ) {
            BasicText("$done / $total 챕터", style = TextSpec(11f, FontWeight.Bold).style())
        }
        Spacer(Modifier.weight(1f))
        BasicText(
            DemoData.BOOK_TITLE.replace("의 ", "의\n"),
            style = TextSpec(28f, FontWeight.Bold, GowunBatang, lineHeightPx = 35f).style(),
        )
        BasicText("Autography", style = TextSpec(13f, FontWeight.Bold, GowunBatang).style(), modifier = Modifier.padding(top = 12.px))
    }
}

private val RowNumber = TextSpec(24f, FontWeight.Bold, tabularNumbers = true)

@Composable
private fun BookChapterRow(chapter: ChapterSummary) {
    Column(Modifier.padding(horizontal = 40.px)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(90.px).padding(horizontal = 4.px),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.px),
        ) {
            BasicText("%02d".format(chapter.number), style = RowNumber.style(AutoColors.InkSecondary), modifier = Modifier.width(46.px))
            SingleLine(chapter.title, TextSpec(32f, FontWeight.Bold), AutoColors.Ink, Modifier.weight(1f))
            SingleLine(
                if (chapter.status == ChapterStatus.DONE) "완료 · ${chapter.storyCount}편" else "시작 전",
                TextSpec(21f, FontWeight.SemiBold),
                AutoColors.InkSecondary,
            )
            CircleIconButton(
                icon = if (chapter.status == ChapterStatus.DONE) AutoIcons.Play else AutoIcons.Plus,
                contentDescription = "${chapter.number}장 (인터뷰 중 사용 불가)",
                onClick = {},
                enabled = false,
                sizePx = 64f,
                iconSizePx = 26f,
            )
        }
        HLine()
    }
}

@Composable
private fun CurrentChapterRow(chapter: ChapterSummary) {
    val book = DemoData.book
    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AutoColors.SurfaceRaised)
                .padding(start = 44.px, top = 22.px, end = 44.px, bottom = 26.px),
            verticalArrangement = Arrangement.spacedBy(14.px),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.px)) {
                BasicText("%02d".format(chapter.number), style = RowNumber.style(), modifier = Modifier.width(46.px))
                SingleLine(chapter.title, TextSpec(40f, FontWeight.ExtraBold), AutoColors.Ink, Modifier.weight(1f))
                StatusChip("지금 쓰는 중", filled = true, horizontalPaddingPx = 14f, verticalPaddingPx = 4f)
            }
            Column(Modifier.padding(start = 66.px), verticalArrangement = Arrangement.spacedBy(10.px)) {
                ProgressBar(chapter.progress)
                BasicText(
                    "${chapter.storyCount}편 · ${(chapter.progress * 100).roundToInt()}% · 질문 ${book.currentQuestion} / ${book.currentQuestionTotal}",
                    style = TextSpec(21f, FontWeight.SemiBold).style(AutoColors.InkSecondary),
                )
            }
        }
        HLine()
    }
}

/** 15초 뒤로: 아이콘 아래에 작은 “15”. */
@Composable
private fun RewindButton() {
    Box(
        modifier = Modifier
            .size(80.px)
            .clip(CircleShape)
            .border(2.5.px, AutoColors.Ink, CircleShape)
            .clickable(role = Role.Button, onClickLabel = "15초 뒤로") {},
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(rememberVectorPainter(AutoIcons.Rewind), "15초 뒤로", Modifier.size(28.px), colorFilter = ColorFilter.tint(AutoColors.Ink))
            BasicText("15", style = TextSpec(12f, FontWeight.ExtraBold, lineHeightPx = 13f).style())
        }
    }
}
