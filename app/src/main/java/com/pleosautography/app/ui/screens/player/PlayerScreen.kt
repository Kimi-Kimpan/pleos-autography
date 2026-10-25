package com.pleosautography.app.ui.screens.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.pleosautography.app.data.demo.DemoData
import com.pleosautography.app.data.audio.RadioPlayer
import com.pleosautography.app.ui.LocalAppServices
import com.pleosautography.app.ui.components.CircleIconButton
import com.pleosautography.app.ui.components.radioPlaying
import com.pleosautography.app.ui.screens.LocalBookState
import com.pleosautography.app.ui.screens.LocalRadioState
import com.pleosautography.app.ui.screens.drive.radioLabel
import com.pleosautography.app.ui.screens.drive.similarStories
import com.pleosautography.app.ui.components.HLine
import com.pleosautography.app.ui.components.SingleLine
import com.pleosautography.app.ui.icons.AutoIcons
import com.pleosautography.app.ui.screens.currentChapter
import com.pleosautography.app.ui.screens.drive.EpisodeRow
import com.pleosautography.app.ui.screens.drive.GenreChip
import com.pleosautography.app.ui.screens.drive.RadioCover
import com.pleosautography.app.ui.screens.drive.RewindButton
import com.pleosautography.app.ui.screens.drive.SearchField
import com.pleosautography.app.ui.screens.drive.SeekBar
import com.pleosautography.app.ui.theme.AutoColors
import com.pleosautography.app.ui.theme.TextSpec
import com.pleosautography.app.ui.theme.px

private val Small = TextSpec(20f, FontWeight.Bold)
private val BookLine = TextSpec(30f, FontWeight.ExtraBold)
private val PaneLabel = TextSpec(18f, FontWeight.ExtraBold, letterSpacingEm = 0.12f)
private val Section = TextSpec(28f, FontWeight.ExtraBold)
private val Meta = TextSpec(22f, FontWeight.SemiBold)

/**
 * 03 오토그래피 라디오(정차 중). 시안: design/screens/Player.png
 * 미니 플레이어를 펼친 화면. 위에는 지금 쓰는 자서전(접기 버튼), 가운데는 재생 중인 이야기, 아래는 검색·장르·다음 이야기.
 * 재생·일시정지·다음·15초 뒤로는 재생 위치를 실제로 움직인다(낭독 음원은 2단계라 소리는 없다).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerScreen(onCollapse: () -> Unit, modifier: Modifier = Modifier, showCollapse: Boolean = true) {
    val radio = DemoData.radio
    val now = LocalRadioState.current
    val player = LocalAppServices.current?.radioPlayer
    val playing = radioPlaying()
    val chapter = currentChapter()
    var genre by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.px, top = 24.px, end = 32.px, bottom = 22.px),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.px),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.px)) {
                BasicText("내 자서전", style = Small.style(AutoColors.InkSecondary))
                SingleLine("${LocalBookState.current.title} · ${chapter.number}장 ${chapter.title} 쓰는 중", BookLine, AutoColors.Ink)
            }
            if (showCollapse) CircleIconButton(AutoIcons.ChevronDown, "내 자서전 열기", onClick = onCollapse, sizePx = 80f, iconSizePx = 36f)
        }
        HLine(color = AutoColors.Ink)

        Column(
            modifier = Modifier.padding(start = 32.px, top = 26.px, end = 32.px, bottom = 32.px),
            verticalArrangement = Arrangement.spacedBy(20.px),
        ) {
            BasicText("AUTOGRAPHY RADIO", style = PaneLabel.style())
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(32.px)) {
                RadioCover(now.episode.volume)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.px)) {
                    SingleLine(radioLabel(radio.nowPlayingLabel), TextSpec(21f, FontWeight.Bold), AutoColors.InkSecondary)
                    SingleLine(now.episode.title, TextSpec(42f, FontWeight.ExtraBold, lineHeightPx = 48.3f), AutoColors.Ink)
                    SingleLine(now.episode.narrator, TextSpec(23f, FontWeight.SemiBold), AutoColors.InkSecondary)
                    Column(Modifier.padding(top = 6.px), verticalArrangement = Arrangement.spacedBy(8.px)) {
                        SeekBar(now.progress, knobPx = 28f)
                        Row(Modifier.fillMaxWidth()) {
                            val time = TextSpec(20f, FontWeight.SemiBold, tabularNumbers = true)
                            BasicText(RadioPlayer.formatTime(now.positionSec), style = time.style(AutoColors.InkSecondary), modifier = Modifier.weight(1f))
                            BasicText(RadioPlayer.formatTime(now.episode.durationSec), style = time.style(AutoColors.InkSecondary))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.px)) {
                        RewindButton(sizePx = 84f, iconPx = 30f, labelPx = 13f, onClick = { player?.rewind15() })
                        CircleIconButton(if (playing) AutoIcons.Pause else AutoIcons.Play, if (playing) "일시정지" else "재생", onClick = { player?.togglePlay() }, sizePx = 112f, iconSizePx = 44f, borderPx = 4f)
                        CircleIconButton(AutoIcons.Next, "다음 이야기", onClick = { player?.next() }, sizePx = 84f, iconSizePx = 32f)
                    }
                }
            }
        }
        HLine(color = AutoColors.Ink)

        Column(
            modifier = Modifier.padding(start = 32.px, top = 26.px, end = 32.px),
            verticalArrangement = Arrangement.spacedBy(18.px),
        ) {
            SearchField(heightPx = 76f, textPx = 24f, iconPx = 30f) {
                Image(rememberVectorPainter(AutoIcons.MicFilled), "말로 검색", Modifier.size(28.px), colorFilter = ColorFilter.tint(AutoColors.InkSecondary))
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.px), verticalArrangement = Arrangement.spacedBy(10.px)) {
                (radio.genres + radio.moreGenres).forEachIndexed { index, name ->
                    GenreChip(
                        name,
                        selected = index == genre,
                        heightPx = 60f,
                        textPx = 22f,
                        modifier = Modifier.clickable(role = Role.Button) { genre = index },
                    )
                }
            }
            Column {
                HLine()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.px)
                        .clickable(role = Role.Button) {}
                        .padding(horizontal = 4.px),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.px),
                ) {
                    Image(rememberVectorPainter(AutoIcons.Heart), null, Modifier.size(32.px), colorFilter = ColorFilter.tint(AutoColors.Ink))
                    BasicText("내 최애 리스트", style = Section.style())
                    BasicText("${radio.favoriteCount}편", style = Meta.style(AutoColors.InkSecondary), modifier = Modifier.weight(1f))
                    Image(rememberVectorPainter(AutoIcons.ChevronRight), null, Modifier.size(30.px), colorFilter = ColorFilter.tint(AutoColors.Ink))
                }
                HLine()
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 32.px, top = 18.px, end = 32.px),
            verticalAlignment = Alignment.Bottom,
        ) {
            BasicText("다음으로 들을 이야기", style = Section.style(), modifier = Modifier.weight(1f))
            BasicText(similarStories(), style = TextSpec(20f, FontWeight.SemiBold).style(AutoColors.InkSecondary))
        }
        Column(Modifier.padding(start = 32.px, top = 4.px, end = 32.px)) {
            now.upNext.forEachIndexed { index, episode ->
                EpisodeRow(episode, heightPx = 116f, gapPx = 20f, titlePx = 28f, metaPx = 20f)
                if (index < now.upNext.lastIndex) HLine()
            }
        }
    }
}
