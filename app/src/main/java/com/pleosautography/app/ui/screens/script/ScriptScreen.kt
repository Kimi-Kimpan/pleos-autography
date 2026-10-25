package com.pleosautography.app.ui.screens.script

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.pleosautography.app.data.demo.DemoData
import com.pleosautography.app.data.demo.ManuscriptParagraph
import com.pleosautography.app.data.repo.AutographyRepository
import com.pleosautography.app.data.speech.CaptionSpeechOutput
import com.pleosautography.app.data.speech.DemoScript
import com.pleosautography.app.data.speech.Heard
import com.pleosautography.app.domain.interview.VoiceCommand
import com.pleosautography.app.domain.listen.FixParser
import com.pleosautography.app.domain.listen.ListenSession
import com.pleosautography.app.ui.LocalAppServices
import com.pleosautography.app.ui.components.HLine
import com.pleosautography.app.ui.components.NoticeHost
import com.pleosautography.app.ui.components.PillButton
import com.pleosautography.app.ui.components.RuledText
import com.pleosautography.app.ui.components.ScreenStatusChip
import com.pleosautography.app.ui.components.VoiceWave
import com.pleosautography.app.ui.components.rememberNoticeState
import com.pleosautography.app.ui.icons.AutoIcons
import com.pleosautography.app.ui.screens.LocalBookState
import com.pleosautography.app.ui.screens.LocalInterviewUi
import com.pleosautography.app.ui.screens.LocalPlayerSheetOpen
import com.pleosautography.app.ui.screens.chapterSummary
import com.pleosautography.app.ui.screens.currentChapter
import com.pleosautography.app.ui.theme.AutoColors
import com.pleosautography.app.ui.theme.GowunBatang
import com.pleosautography.app.ui.theme.TextSpec
import com.pleosautography.app.ui.theme.px
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Title = TextSpec(52f, FontWeight.ExtraBold, lineHeightPx = 57.2f)
private val Hint = TextSpec(24f, FontWeight.SemiBold)
private val Body = TextSpec(34f, FontWeight.Normal, GowunBatang, lineHeightPx = 58f)
private val ContinueAsk = TextSpec(36f, FontWeight.Bold, GowunBatang, lineHeightPx = 46.8f, letterSpacingEm = -0.01f)
private val Floating = TextSpec(27f, FontWeight.ExtraBold)
private val Caption = TextSpec(20f, FontWeight.SemiBold)
private val WriteHint = TextSpec(26f, FontWeight.SemiBold)
private val FixInput = TextSpec(30f, FontWeight.Bold, lineHeightPx = 42f)

/** 원고 보기에서 지금 하는 일. */
private enum class ScriptMode {
    VIEW,

    /** 이어쓰기: 원고 끝 입력 줄에 말한 문장이 적힌다. */
    WRITING,

    /** 챕터 듣기: 원고를 옅게 두고 오토가 읽는 어절을 따라 칠한다. */
    PLAYING,
}

/** 챕터 듣기에서 읽는 자리(문단·문장·어절). */
private data class ReadingSpot(val paragraph: Int, val sentence: Int, val word: Int)

/** 말로 고치기 미리보기: 이 문단의 [wrong]을 [fixed]로 바꾼다. */
private data class FixPreview(val wrong: String, val fixed: String)

/** 이 색(약 40%)으로 원고 전체를 옅게 둔다(챕터 듣기). */
private val Dimmed = AutoColors.Ink.copy(alpha = 0.4f)

/** 받아 적은 문장을 타이핑하듯 보이는 속도(글자당). */
private const val TYPING_MS = 45L

/**
 * 05 원고 보기(정차 중). 시안: design/screens/Script.png
 * 노트 줄 위 원고. 저장한 답변이 있으면 그 답변을, 없으면 예시 원고를 보인다. 모든 일을 이 화면 안에서 한다.
 * - 문단을 길게 누르면 말로 고치기·다시 듣기·지우기.
 *   - 말로 고치기: 문단 아래 입력창에 말한 정정이 적히고, 원고에 취소선 → 새 말로 미리 보인 뒤 확인하면 저장한다.
 *   - 지우기: 답변을 지우고 4초 동안 되돌리기를 보인다.
 * - 이어쓰기: 원고 끝에 깜빡이는 커서와 작은 파형. 말하면 그 자리에 문장이 적히고 저장된다. ‘여기까지’로 멈춘다.
 * - 챕터 듣기: 원고를 옅게 두고 오토가 읽는 어절을 따라 진하게 칠하며 읽는 자리로 스크롤한다. 라디오는 멈춘다.
 * 받아 적기는 음성 입력 어댑터를 쓴다(시연은 실제 마이크 음량 + 준비된 문장).
 * @param showHome 가로 전체 창에서는 왼쪽에 홈이 늘 있으므로 ‘홈’ 버튼을 뺀다.
 * @param chapterNumber 열 챕터(홈에서 완료한 챕터를 골랐을 때). null이면 인터뷰 중 챕터나 지금 쓰는 챕터.
 */
@Composable
fun ScriptScreen(
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
    showHome: Boolean = true,
    chapterNumber: Int? = null,
) {
    // 고른 챕터(완료한 챕터 줄), 인터뷰에서 열었으면 그 챕터, 아니면 지금 쓰는 챕터.
    val interviewChapter = LocalInterviewUi.current.chapter
    val chapter = when {
        chapterNumber != null -> chapterSummary(chapterNumber)
        interviewChapter > 0 -> chapterSummary(interviewChapter)
        else -> currentChapter()
    }
    val services = LocalAppServices.current
    val bookId = LocalBookState.current.id
    val answers = remember(services, chapter.number) { services?.answersFor?.invoke(chapter.number) }
        ?.collectAsState(initial = null)?.value
    var mode by rememberSaveable { mutableStateOf(ScriptMode.VIEW) }
    // 이어쓰기에서 막 저장해 아직 타이핑 중인 답변은 목록에서 잠시 숨긴다(입력 줄에 보이는 중).
    var typingAnswerId by remember { mutableStateOf<Long?>(null) }
    val example = answers?.isEmpty() == true
    val demoFix = remember(services) { services?.demoFixSentence?.let(FixParser::parse) }
    val paragraphs = when {
        answers == null -> emptyList()
        example -> DemoData.manuscript
        else -> answers.filter { it.id != typingAnswerId }.map { answer ->
            // 시연용 정정 대상(“1988년”)이 든 문단은 길게 누를 때 그 구절을 칠한다(시안).
            val highlight = demoFix?.takeIf { it.wrong in answer.transcript }?.let { ListenSession.phraseAround(answer.transcript, it.wrong) }
            ManuscriptParagraph(answer.transcript, highlight, answer.id)
        }
    }
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    var reading by remember { mutableStateOf<Int?>(null) }
    var readingSentence by remember { mutableIntStateOf(0) }
    var spot by remember { mutableStateOf<ReadingSpot?>(null) }
    var fixing by remember { mutableStateOf<Int?>(null) }
    var fixHeard by remember { mutableStateOf("") }
    var fixPreview by remember { mutableStateOf<FixPreview?>(null) }
    var fixProblem by remember { mutableStateOf<String?>(null) }
    var typed by remember { mutableStateOf("") }
    var level by remember { mutableFloatStateOf(0f) }
    val notice = rememberNoticeState()
    val scroll = rememberScrollState()
    val paragraphTops = remember { mutableStateMapOf<Int, Int>() }

    // 플레이어 시트가 열리면 마이크·낭독을 멈춘다. 이어쓰기·말로 고치기는 받아 적은 데까지 이미 저장했으니 입력창만 닫는다.
    val sheetOpen = LocalPlayerSheetOpen.current
    LaunchedEffect(sheetOpen) {
        if (!sheetOpen) return@LaunchedEffect
        mode = ScriptMode.VIEW
        reading = null
        // 말로 고치기를 다 들은 뒤라면(미리보기까지) 그 고침을 저장한다.
        val preview = fixPreview
        val paragraph = fixing?.let { paragraphs.getOrNull(it) }
        val id = paragraph?.answerId
        if (preview != null && paragraph != null && id != null) {
            services?.launch { services.repository.updateTranscript(id, paragraph.text.replaceFirst(preview.wrong, preview.fixed)) }
        }
        fixing = null
        fixHeard = ""
        fixPreview = null
        fixProblem = null
    }

    // 다시 듣기(한 문단): 문장마다 읽는 시간만큼 칠한다.
    LaunchedEffect(reading) {
        val text = reading?.let { paragraphs.getOrNull(it)?.text } ?: return@LaunchedEffect
        ListenSession.splitSentences(text).forEachIndexed { i, sentence ->
            readingSentence = i
            services?.readAloud?.invoke(sentence)
        }
        reading = null
    }

    // 챕터 듣기: 원고 전체를 어절 단위로 따라 읽는다. 라디오는 낭독 중 멈춘다.
    LaunchedEffect(mode == ScriptMode.PLAYING) {
        if (mode != ScriptMode.PLAYING) return@LaunchedEffect
        services?.setTalking?.invoke(true)
        try {
            paragraphs.forEachIndexed { p, paragraph ->
                ListenSession.splitSentences(paragraph.text).forEachIndexed { s, sentence ->
                    coroutineScope {
                        // 소리(TTS)는 문장 단위로 내고, 화면은 어절마다 읽는 시간만큼 넘어간다.
                        launch { services?.readAloud?.invoke(sentence) }
                        sentence.split(' ').forEachIndexed { w, word ->
                            spot = ReadingSpot(p, s, w)
                            delay(CaptionSpeechOutput.estimateMs(word) - 300L)
                        }
                    }
                }
            }
        } finally {
            services?.setTalking?.invoke(false)
            spot = null
            mode = ScriptMode.VIEW
        }
    }
    // 읽는 문단으로 스크롤한다.
    LaunchedEffect(spot?.paragraph) {
        val p = spot?.paragraph ?: return@LaunchedEffect
        paragraphTops[p]?.let { scroll.animateScrollTo((it - 120).coerceAtLeast(0)) }
    }

    // 이어쓰기: 말하면 받아 적어 원고 끝에 타이핑하듯 보이고 저장한다. “여기까지”나 버튼으로 멈춘다.
    LaunchedEffect(mode == ScriptMode.WRITING) {
        if (mode != ScriptMode.WRITING || services == null || bookId == null) return@LaunchedEffect
        services.setTalking(true)
        scroll.animateScrollTo(scroll.maxValue)
        try {
            while (true) {
                val question = services.repository.observeProgress(bookId, chapter.number).first().nextQuestion
                val heard = services.speechInput.listen(question?.id ?: AutographyRepository.extraQuestionId(chapter.number)) { level = it }
                level = 0f
                if (heard !is Heard.Text) continue
                when (VoiceCommand.parseCommand(heard.text)) {
                    null -> Unit
                    VoiceCommand.STOP -> break
                    else -> continue // 다른 명령은 원고에 넣지 않는다.
                }
                // 먼저 저장하고(데이터 유실 없음) 입력 줄에서 타이핑하듯 보인다.
                typingAnswerId = if (question != null) {
                    services.repository.saveAnswer(bookId, question, heard.text, heard.durationSec)
                } else {
                    services.repository.saveExtra(bookId, chapter.number, heard.text, heard.durationSec)
                }
                for (i in 1..heard.text.length) {
                    typed = heard.text.take(i)
                    delay(TYPING_MS)
                    if (i % 12 == 0) scroll.scrollTo(scroll.maxValue)
                }
                delay(400)
                typed = ""
                typingAnswerId = null
                scroll.animateScrollTo(scroll.maxValue)
            }
        } finally {
            typed = ""
            typingAnswerId = null
            level = 0f
            services.setTalking(false)
            mode = ScriptMode.VIEW
        }
    }

    // 말로 고치기: 고른 문단 아래 입력창에 말한 정정이 적히고, 원고에 미리 보인다.
    LaunchedEffect(fixing) {
        val index = fixing ?: return@LaunchedEffect
        val paragraph = paragraphs.getOrNull(index) ?: return@LaunchedEffect
        services ?: return@LaunchedEffect
        services.setTalking(true)
        try {
            while (fixPreview == null) {
                val heard = services.speechInput.listen(DemoScript.FIX_ID) { level = it }
                level = 0f
                if (heard !is Heard.Text) continue
                for (i in 1..heard.text.length) {
                    fixHeard = heard.text.take(i)
                    delay(TYPING_MS)
                }
                val parsed = FixParser.parse(heard.text)
                when {
                    parsed == null -> fixProblem = "어느 부분을 무엇으로 고칠지 다시 말씀해 주세요. 예) “A가 아니라 B였어”"
                    parsed.wrong !in paragraph.text -> fixProblem = "‘${parsed.wrong}’을 이 문단에서 찾지 못했어요. 다시 말씀해 주세요."
                    else -> {
                        fixProblem = null
                        fixPreview = FixPreview(parsed.wrong, parsed.fixed)
                    }
                }
            }
        } finally {
            level = 0f
            services.setTalking(false)
        }
    }
    val closeFix = {
        fixing = null
        fixHeard = ""
        fixPreview = null
        fixProblem = null
    }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.px, top = 32.px, end = 32.px, bottom = 28.px),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.px),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.px)) {
                    ScreenStatusChip("정차 중 · P", filled = false)
                    BasicText("${chapter.number}장 ${chapter.title} · 원고", style = Title.style(), maxLines = 1)
                }
                if (showHome) PillButton(
                    text = "홈",
                    onClick = onHome,
                    icon = AutoIcons.ChevronLeft,
                    heightPx = 100f,
                    iconSizePx = 32f,
                    startPaddingPx = 26f,
                    endPaddingPx = 36f,
                    gapPx = 10f,
                    textSpec = TextSpec(34f, FontWeight.ExtraBold),
                )
            }
            HLine(color = AutoColors.Ink)

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(scroll)
                    .padding(start = 32.px, top = 8.px, end = 32.px),
            ) {
                Row(
                    modifier = Modifier.padding(top = 16.px, bottom = 6.px),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.px),
                ) {
                    Image(rememberVectorPainter(AutoIcons.Select), null, Modifier.size(26.px), colorFilter = ColorFilter.tint(AutoColors.InkSecondary))
                    BasicText(
                        when (mode) {
                            ScriptMode.PLAYING -> "오토가 읽고 있어요 · 읽는 자리를 따라가요"
                            ScriptMode.WRITING -> "말씀하시는 대로 원고 끝에 이어서 적어요"
                            ScriptMode.VIEW -> "고칠 부분을 길게 눌러 고르면, 말로 고칠 수 있어요"
                        },
                        style = Hint.style(AutoColors.InkSecondary),
                    )
                }
                paragraphs.forEachIndexed { index, paragraph ->
                    val fixHere = fixing == index
                    Column(Modifier.onGloballyPositioned { paragraphTops[index] = it.positionInParent().y.toInt() }) {
                        Paragraph(
                            paragraph = paragraph,
                            first = index == 0,
                            selected = selected == index && !fixHere && mode == ScriptMode.VIEW,
                            readingSentence = if (reading == index) readingSentence else null,
                            karaoke = spot,
                            karaokeOn = mode == ScriptMode.PLAYING,
                            index = index,
                            preview = if (fixHere) fixPreview else null,
                            onLongPress = {
                                if (mode == ScriptMode.VIEW && fixing == null) selected = if (selected == index) null else index
                            },
                            actions = ParagraphActions(
                                reading = reading == index,
                                onFix = {
                                    if (paragraph.answerId == null) {
                                        notice.show("예시 원고는 고칠 수 없어요")
                                    } else {
                                        reading = null
                                        fixing = index
                                    }
                                    selected = null
                                },
                                onListen = { reading = if (reading == index) null else index },
                                onDelete = {
                                    val answer = answers?.firstOrNull { it.id == paragraph.answerId }
                                    if (answer != null) {
                                        services?.launch { services.repository.deleteAnswer(answer.id) }
                                        notice.show("문단을 지웠어요", actionLabel = "되돌리기") {
                                            services?.launch { services.repository.restoreAnswer(answer) }
                                        }
                                    } else {
                                        notice.show("예시 원고는 지울 수 없어요")
                                    }
                                    selected = null
                                    reading = null
                                },
                            ),
                        )
                        if (fixHere) {
                            FixBox(
                                heard = fixHeard,
                                level = level,
                                problem = fixProblem,
                                ready = fixPreview != null,
                                onCancel = closeFix,
                                onConfirm = {
                                    val preview = fixPreview
                                    val id = paragraph.answerId
                                    if (preview != null && id != null) {
                                        val fixed = paragraph.text.replaceFirst(preview.wrong, preview.fixed)
                                        services?.launch { services.repository.updateTranscript(id, fixed) }
                                        val phrase = ListenSession.phraseAround(fixed, preview.fixed)
                                        notice.show("‘$phrase’${ListenSession.directionParticle(phrase)} 고쳤어요")
                                    }
                                    closeFix()
                                },
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dashedBottomBorder()
                        .padding(vertical = 28.px),
                    verticalArrangement = Arrangement.spacedBy(20.px),
                ) {
                    if (mode == ScriptMode.WRITING) {
                        WritingLine(typed = typed, level = level, onStop = { mode = ScriptMode.VIEW })
                    } else {
                        BasicText("여기서부터 이어서 써 볼까요?", style = ContinueAsk.style())
                        Row(horizontalArrangement = Arrangement.spacedBy(16.px)) {
                            PillButton(
                                "이어쓰기",
                                {
                                    closeFix()
                                    selected = null
                                    reading = null
                                    mode = ScriptMode.WRITING
                                },
                                icon = AutoIcons.Pencil,
                                filled = true,
                                enabled = mode == ScriptMode.VIEW && bookId != null,
                            )
                            PillButton(
                                if (mode == ScriptMode.PLAYING) "멈추기" else "챕터 듣기",
                                {
                                    if (mode == ScriptMode.PLAYING) {
                                        mode = ScriptMode.VIEW
                                    } else {
                                        closeFix()
                                        selected = null
                                        reading = null
                                        mode = ScriptMode.PLAYING
                                    }
                                },
                                icon = if (mode == ScriptMode.PLAYING) AutoIcons.Pause else AutoIcons.Play,
                                iconSizePx = 26f,
                                enabled = paragraphs.isNotEmpty(),
                            )
                        }
                    }
                }
                if (example) {
                    BasicText("예시 원고", style = Caption.style(AutoColors.InkSecondary), modifier = Modifier.padding(top = 14.px))
                }
                Spacer(Modifier.height(32.px))
            }
        }
        NoticeHost(notice, Modifier.align(Alignment.BottomCenter))
    }
}

/** 고른 문단 위 알약 버튼의 동작. */
private class ParagraphActions(val reading: Boolean, val onFix: () -> Unit, val onListen: () -> Unit, val onDelete: () -> Unit)

/**
 * 원고 한 문단. 고르면 강조 구절(없으면 문단 전체)을 파랗게 칠하고 위에 알약 버튼을 띄운다.
 * @param readingSentence 다시 듣기 중이면 지금 읽는 문장 번호(연보라로 칠한다).
 * @param karaokeOn 챕터 듣기 중: 전체를 옅게 두고 [karaoke] 자리의 문장을 읽은 어절까지 진하게.
 * @param preview 말로 고치기 미리보기(취소선 → 새 말).
 */
@Composable
private fun Paragraph(
    paragraph: ManuscriptParagraph,
    first: Boolean,
    selected: Boolean,
    readingSentence: Int?,
    karaoke: ReadingSpot?,
    karaokeOn: Boolean,
    index: Int,
    preview: FixPreview?,
    onLongPress: () -> Unit,
    actions: ParagraphActions,
) {
    val sentences = ListenSession.splitSentences(paragraph.text)
    val text = when {
        karaokeOn -> karaokeText(sentences, karaoke?.takeIf { it.paragraph == index })
        preview != null -> previewText(paragraph.text, preview)
        readingSentence != null && readingSentence in sentences.indices -> buildAnnotatedString {
            sentences.forEachIndexed { i, sentence ->
                if (i > 0) append(" ")
                if (i == readingSentence) withStyle(SpanStyle(background = AutoColors.LavenderSoft)) { append(sentence) } else append(sentence)
            }
        }
        else -> buildAnnotatedString {
            val target = if (selected) paragraph.highlight ?: paragraph.text else null
            val start = target?.let { paragraph.text.indexOf(it) } ?: -1
            if (target == null || start < 0) {
                append(paragraph.text)
            } else {
                append(paragraph.text.substring(0, start))
                withStyle(SpanStyle(background = AutoColors.Ink, color = AutoColors.OnInk)) { append(target) }
                append(paragraph.text.substring(start + target.length))
            }
        }
    }
    Box(Modifier.pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPress() }) }) {
        RuledText(text, Body, Modifier.fillMaxWidth())
        if (selected) {
            // 문단 위 오른쪽에 띄운다. 첫 문단은 위가 머리글에 가려지므로 아래에 띄운다.
            Row(
                modifier = Modifier
                    .align(if (first) Alignment.BottomEnd else Alignment.TopEnd)
                    .wrapContentSize()
                    .offset(y = if (first) 86.px else (-86).px),
                horizontalArrangement = Arrangement.spacedBy(12.px),
            ) {
                FloatingPill("말로 고치기", filled = true, onClick = actions.onFix)
                FloatingPill(if (actions.reading) "멈추기" else "다시 듣기", filled = false, onClick = actions.onListen)
                FloatingPill("지우기", filled = false, onClick = actions.onDelete)
            }
        }
    }
}

/** 노래방 자막처럼: 전체는 옅게, 읽는 문장은 읽은 어절까지 진하게. */
private fun karaokeText(sentences: List<String>, spot: ReadingSpot?): AnnotatedString = buildAnnotatedString {
    sentences.forEachIndexed { s, sentence ->
        if (s > 0) append(" ")
        if (spot == null || spot.sentence != s) {
            withStyle(SpanStyle(color = Dimmed)) { append(sentence) }
        } else {
            sentence.split(' ').forEachIndexed { w, word ->
                if (w > 0) append(" ")
                withStyle(SpanStyle(color = if (w <= spot.word) AutoColors.Ink else Dimmed)) { append(word) }
            }
        }
    }
}

/** 고칠 말은 취소선(옅게), 바로 뒤에 새 말을 파랗게 칠한다. */
private fun previewText(text: String, preview: FixPreview): AnnotatedString = buildAnnotatedString {
    val at = text.indexOf(preview.wrong)
    if (at < 0) {
        append(text)
        return@buildAnnotatedString
    }
    append(text.substring(0, at))
    withStyle(SpanStyle(color = AutoColors.Ink.copy(alpha = 0.5f), textDecoration = TextDecoration.LineThrough)) { append(preview.wrong) }
    append(" ")
    withStyle(SpanStyle(background = AutoColors.Ink, color = AutoColors.OnInk)) { append(preview.fixed) }
    append(text.substring(at + preview.wrong.length))
}

/** 깜빡이는 커서(0.5초). */
@Composable
private fun rememberBlink(): Boolean {
    var on by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(530)
            on = !on
        }
    }
    return on
}

/** 이어쓰기 입력 줄: 적히는 문장 + 깜빡이는 커서, 아래에 작은 파형·안내·‘여기까지’. */
@Composable
private fun WritingLine(typed: String, level: Float, onStop: () -> Unit) {
    val blink = rememberBlink()
    RuledText(
        buildAnnotatedString {
            append(typed)
            withStyle(SpanStyle(color = if (blink) AutoColors.Ink else Color.Transparent)) { append("│") }
        },
        Body,
        Modifier.fillMaxWidth(),
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.px)) {
        VoiceWave(levels = smallWave(level), barWidthPx = 7f, gapPx = 6f, heightPx = 56f)
        BasicText(
            if (typed.isEmpty()) "말씀하시면 여기에 이어서 적어요" else "받아 적고 있어요",
            style = WriteHint.style(AutoColors.InkMuted),
            modifier = Modifier.weight(1f),
        )
        PillButton("여기까지", onStop, icon = AutoIcons.Clip, filled = true, heightPx = 84f, iconSizePx = 30f, horizontalPaddingPx = 30f)
    }
}

/** 말로 고치기 입력창: 둥근 테두리, 들은 말 또는 옅은 안내 + 커서, 오른쪽 마이크. 아래에 취소·확인. */
@Composable
private fun FixBox(heard: String, level: Float, problem: String?, ready: Boolean, onCancel: () -> Unit, onConfirm: () -> Unit) {
    val blink = rememberBlink()
    val shape = RoundedCornerShape(36.px)
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 14.px, bottom = 22.px),
        verticalArrangement = Arrangement.spacedBy(14.px),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.px)
                .background(AutoColors.SurfacePill, shape)
                .border(2.5.px, AutoColors.Ink, shape)
                .padding(start = 30.px, top = 18.px, end = 22.px, bottom = 18.px),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.px),
        ) {
            BasicText(
                buildAnnotatedString {
                    if (heard.isEmpty()) {
                        withStyle(SpanStyle(color = if (blink) AutoColors.Ink else Color.Transparent)) { append("│") }
                        withStyle(SpanStyle(color = AutoColors.InkMuted)) { append(" 고칠 내용을 말씀해 주세요") }
                    } else {
                        append(heard)
                        if (!ready) withStyle(SpanStyle(color = if (blink) AutoColors.Ink else Color.Transparent)) { append("│") }
                    }
                },
                style = FixInput.style(),
                modifier = Modifier.weight(1f),
            )
            if (!ready) VoiceWave(levels = smallWave(level), barWidthPx = 5f, gapPx = 4f, heightPx = 40f)
            Image(rememberVectorPainter(AutoIcons.MicFilled), "말로 고치기", Modifier.size(34.px), colorFilter = ColorFilter.tint(AutoColors.Ink))
        }
        problem?.let { BasicText(it, style = Hint.style(AutoColors.InkSecondary)) }
        Row(horizontalArrangement = Arrangement.spacedBy(14.px)) {
            PillButton("취소", onCancel, heightPx = 76f, horizontalPaddingPx = 30f)
            PillButton("확인", onConfirm, icon = AutoIcons.Check, filled = true, enabled = ready, heightPx = 76f, iconSizePx = 26f, horizontalPaddingPx = 30f)
        }
    }
}

/** 작은 파형: 조용할 때도 쉬는 모양, 목소리에 따라 커진다. */
private fun smallWave(level: Float): List<Float> {
    val scale = 0.35f + 0.65f * level
    return listOf(0.3f, 0.6f, 0.9f, 0.55f, 1f, 0.7f, 0.4f).map { it * scale }
}

/** 원고 위에 떠 있는 알약 버튼(높이 76). */
@Composable
private fun FloatingPill(text: String, filled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(percent = 50)
    PillButton(
        text = text,
        onClick = onClick,
        modifier = Modifier
            .shadow(if (filled) 8.px else 6.px, shape, ambientColor = ShadowInk, spotColor = ShadowInk)
            .background(AutoColors.SurfacePill, shape),
        icon = if (filled) AutoIcons.MicFilled else null,
        filled = filled,
        heightPx = 76f,
        iconSizePx = 26f,
        horizontalPaddingPx = 26f,
        textSpec = Floating,
    )
}

private val ShadowInk = Color(0x291A0BD0)

/** 시안의 2.5px 점선 아래 선. */
@Composable
private fun Modifier.dashedBottomBorder(): Modifier {
    val width = 2.5.px
    val dash = 8.px
    return drawBehind {
        val w = width.toPx()
        drawLine(
            color = AutoColors.Ink,
            start = Offset(0f, size.height - w / 2),
            end = Offset(size.width, size.height - w / 2),
            strokeWidth = w,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), dash.toPx() * 0.75f)),
        )
    }
}
