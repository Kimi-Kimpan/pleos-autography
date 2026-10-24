package com.pleosautography.app.ui.screens.drive

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.runtime.collectAsState
import com.pleosautography.app.domain.listen.ListenUi
import com.pleosautography.app.ui.LocalAppServices
import com.pleosautography.app.ui.screens.LocalBookState
import com.pleosautography.app.ui.screens.chapterSummary
import com.pleosautography.app.ui.components.BigMicButton
import com.pleosautography.app.ui.components.CircleIconButton
import com.pleosautography.app.ui.components.HLine
import com.pleosautography.app.ui.components.ProgressBar
import com.pleosautography.app.ui.components.RuledText
import com.pleosautography.app.ui.components.ScreenStatusChip
import com.pleosautography.app.ui.components.StopHereButton
import com.pleosautography.app.ui.icons.AutoIcons
import com.pleosautography.app.ui.theme.AutoColors
import com.pleosautography.app.ui.theme.GowunBatang
import com.pleosautography.app.ui.theme.TextSpec
import com.pleosautography.app.ui.theme.px

private val Title = TextSpec(40f, FontWeight.ExtraBold)
private val Reading = TextSpec(22f, FontWeight.Bold)
private val Previous = TextSpec(30f, FontWeight.Normal, GowunBatang, lineHeightPx = 48f)
private val Current = TextSpec(42f, FontWeight.Bold, GowunBatang, lineHeightPx = 64f)
private val UserSaid = TextSpec(28f, FontWeight.Bold, lineHeightPx = 40.6f)
private val OtoSaid = TextSpec(32f, FontWeight.Bold, GowunBatang, lineHeightPx = 46.4f)
private val TellOto = TextSpec(30f, FontWeight.Bold, GowunBatang, lineHeightPx = 45f)

/**
 * 09 원고 듣기·고치기(주행 중). 시안: design/screens/ListenDrive.png
 * 오토가 지금 챕터 원고를 문장 단위로 읽어 주고([ListenSession]), “방금 거기 고치자”로 읽던 문장을 말로 고친다.
 * 고친 자리는 지운 줄 + 파란 칠로 보이고, 고친 문장은 답변에 바로 저장된다(editedAt).
 * @param onCallOto 마이크: 고칠 내용을 말한다(시연은 준비된 정정 문장).
 */
@Composable
fun ListenDriveScreen(onStopHere: () -> Unit, onCallOto: () -> Unit, modifier: Modifier = Modifier) {
    val services = LocalAppServices.current
    val ui = services?.listen?.ui?.collectAsState()?.value ?: ListenUi()
    val chapter = chapterSummary(ui.chapter.takeIf { it > 0 } ?: LocalBookState.current.current)
    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.px, top = 32.px, end = 32.px, bottom = 28.px),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.px),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.px)) {
                ScreenStatusChip("주행 중 · 내 원고 고치기", filled = true, sizePx = 22f)
                BasicText("${chapter.number}장 ${chapter.title}", style = Title.style(), maxLines = 1)
            }
            StopHereButton(onStopHere)
        }
        HLine(color = AutoColors.Ink)

        // 읽고 있는 자리: 앞 문장은 흐리게, 지금 문장은 크게.
        Column(
            modifier = Modifier.padding(start = 32.px, top = 30.px, end = 32.px, bottom = 26.px),
            verticalArrangement = Arrangement.spacedBy(18.px),
        ) {
            val time = "${formatMs(ui.elapsedMs)} / ${formatMs(ui.totalMs)}"
            val status = when {
                ui.sentences.isEmpty() -> "원고를 불러오고 있어요"
                ui.finished -> "끝까지 읽었어요 · $time"
                ui.playing -> "오토가 읽고 있어요 · $time"
                else -> "잠시 멈춤 · $time"
            }
            BasicText(status, style = Reading.style(AutoColors.InkSecondary))
            ui.previous?.let { RuledText(it.text, Previous, Modifier.fillMaxWidth().alpha(0.45f)) }
            ui.current?.let { sentence ->
                val fix = ui.fix?.takeIf { it.sentence == ui.index }
                RuledText(
                    text = buildAnnotatedString {
                        val at = fix?.let { sentence.text.indexOf(it.fixed) } ?: -1
                        if (fix == null || at < 0) {
                            append(sentence.text)
                        } else {
                            append(sentence.text.substring(0, at))
                            withStyle(SpanStyle(color = AutoColors.Ink.copy(alpha = 0.5f), textDecoration = TextDecoration.LineThrough)) {
                                append(fix.wrong)
                            }
                            append(" ")
                            withStyle(SpanStyle(background = AutoColors.Ink, color = AutoColors.OnInk)) { append(fix.fixed) }
                            append(sentence.text.substring(at + fix.fixed.length))
                        }
                    },
                    spec = Current,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        HLine()

        // 말로 고친 대화. 아직 고친 적이 없으면 고치는 방법을 알려 준다.
        BoxWithConstraints(Modifier.fillMaxWidth().padding(start = 32.px, top = 28.px, end = 32.px, bottom = 26.px)) {
            val width = maxWidth
            val exchange = ui.exchange
            if (exchange == null) {
                BasicText(
                    "읽다가 틀린 곳이 있으면\n“방금 거기 고치자”라고 말씀해 주세요.\n예) “1988년이 아니라 1987년이었어.”",
                    style = OtoSaid.style(AutoColors.InkSecondary),
                )
            } else {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(22.px)) {
                    BasicText(
                        text = exchange.userSaid,
                        style = UserSaid.style(AutoColors.OnInk),
                        modifier = Modifier
                            .align(Alignment.End)
                            .widthIn(max = width * 0.78f)
                            .clip(RoundedCornerShape(30.px, 30.px, 8.px, 30.px))
                            .background(AutoColors.Ink)
                            .padding(horizontal = 26.px, vertical = 20.px),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.px), verticalAlignment = Alignment.Bottom) {
                        OtoAvatar()
                        val shape = RoundedCornerShape(30.px, 30.px, 30.px, 8.px)
                        BasicText(
                            text = exchange.otoReply,
                            style = OtoSaid.style(),
                            modifier = Modifier
                                .widthIn(max = width * 0.84f)
                                .clip(shape)
                                .background(AutoColors.SurfacePill)
                                .border(2.px, AutoColors.Line, shape)
                                .padding(horizontal = 26.px, vertical = 20.px),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.px, end = 58.px, bottom = 46.px),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(52.px, Alignment.End),
        ) {
            BasicText(
                "고칠 내용이 있으면\n오토에게 말로 알려 주세요",
                style = TellOto.style().copy(textAlign = TextAlign.End),
            )
            BigMicButton(onCallOto, sizePx = 140f, haloPx = 26f, iconSizePx = 58f)
        }

        // 낭독 플레이어.
        HLine(color = AutoColors.Ink)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.px, top = 22.px, end = 32.px, bottom = 30.px),
            horizontalArrangement = Arrangement.spacedBy(20.px),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProgressBar(if (ui.totalMs == 0L) 0f else ui.elapsedMs.toFloat() / ui.totalMs, Modifier.weight(1f))
            CircleIconButton(
                if (ui.playing) AutoIcons.Pause else AutoIcons.Play,
                if (ui.playing) "일시정지" else "이어 듣기",
                onClick = { services?.listen?.togglePlay() },
                sizePx = 92f,
                iconSizePx = 34f,
                borderPx = 3.5f,
            )
            CircleIconButton(AutoIcons.Next, "다음 문단", onClick = { services?.listen?.nextParagraph() }, sizePx = 84f, iconSizePx = 32f)
        }
    }
}

private fun formatMs(ms: Long): String {
    val sec = ms / 1000
    return "%02d:%02d".format(sec / 60, sec % 60)
}

/** 오토 얼굴: 연보라 원 안의 물결(시안 SVG viewBox 40×24, 두께 3). */
@Composable
private fun OtoAvatar() {
    Box(
        Modifier
            .size(60.px)
            .clip(CircleShape)
            .background(AutoColors.LavenderSoft),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(width = 34.px, height = 20.px)) {
            val sx = size.width / 40f
            val sy = size.height / 24f
            val path = Path().apply {
                moveTo(2f * sx, 12f * sy)
                quadraticTo(7f * sx, 2f * sy, 12f * sx, 12f * sy)
                // T: 앞 제어점을 반사한다.
                var cx = 17f
                var cy = 22f
                var x = 22f
                while (x <= 42f) {
                    quadraticTo(cx * sx, cy * sy, x * sx, 12f * sy)
                    cy = 24f - cy
                    cx += 10f
                    x += 10f
                }
            }
            drawPath(path, AutoColors.Ink, style = Stroke(width = 3f * sx, cap = StrokeCap.Round))
        }
    }
}
