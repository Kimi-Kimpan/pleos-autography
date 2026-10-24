package com.pleosautography.app.ui.screens.onboarding

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.core.content.ContextCompat
import com.pleosautography.app.data.demo.DemoData
import com.pleosautography.app.ui.components.RuledText
import com.pleosautography.app.ui.components.StatusChip
import com.pleosautography.app.ui.icons.AutoIcons
import com.pleosautography.app.ui.theme.AutoColors
import com.pleosautography.app.ui.theme.GowunBatang
import com.pleosautography.app.ui.theme.TextSpec
import com.pleosautography.app.ui.theme.px

private val Greeting = TextSpec(54f, FontWeight.Bold, GowunBatang, lineHeightPx = 78f, letterSpacingEm = -0.015f)
private val Label = TextSpec(26f, FontWeight.ExtraBold)
private val Field = TextSpec(32f, FontWeight.Bold)
private val Chip = TextSpec(26f, FontWeight.Bold)
private val ChapterChip = TextSpec(30f, FontWeight.Bold)
private val ChapterNumber = TextSpec(22f, FontWeight.Bold, tabularNumbers = true)
private val Note = TextSpec(22f, FontWeight.SemiBold)
private val Nav = TextSpec(32f, FontWeight.Bold, GowunBatang)
private val Step = TextSpec(32f, FontWeight.Bold, GowunBatang, tabularNumbers = true)

private const val OWNER_NAME = "김○○"
private const val PICK_FOR_ME = 0

/**
 * 11 첫 실행(정차 중) 세 단계. 시안: design/screens/Onboarding.png, Onboarding2.png, Onboarding3.png
 * 1 부르는 이름 + 마이크 허용, 2 자서전 제목, 3 첫 챕터. 입력값 저장은 Settings 테이블을 붙일 때 연결한다.
 * @param step 1~3.
 */
@Composable
fun OnboardingScreen(
    step: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(40.px),
    ) {
        StatusChip("처음 시작하기 · 정차 중", sizePx = 22f)
        when (step) {
            1 -> NameStep()
            2 -> TitleStep()
            else -> ChapterStep()
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.px)) {
            BasicText("$step/3", style = Step.style(AutoColors.InkMuted), modifier = Modifier.weight(1f))
            NavButton("이전", filled = false, enabled = step > 1, onClick = onPrevious)
            NavButton(if (step == 3) "시작하기" else "다음", filled = true, onClick = onNext)
        }
        if (step == 1) MicPermissionRow(Modifier.padding(top = 40.px))
    }
}

@Composable
private fun Question(text: String) {
    RuledText(text, Greeting, Modifier.fillMaxWidth().padding(top = 220.px))
}

@Composable
private fun NameStep() {
    var name by rememberSaveable { mutableStateOf(OWNER_NAME) }
    var suffix by rememberSaveable { mutableIntStateOf(0) }
    Column {
        Question("반가워요, 저는 오토예요.\n운전하시는 동안 이야기를 듣고\n자서전으로 받아 적어 드릴게요.")
        Column(Modifier.padding(top = 56.px), verticalArrangement = Arrangement.spacedBy(16.px)) {
            BasicText("어떻게 불러 드릴까요?", style = Label.style())
            InputField(name, onChange = { name = it })
            Row(horizontalArrangement = Arrangement.spacedBy(12.px)) {
                listOf("님", " 기사님", " 사장님").forEachIndexed { index, it ->
                    ChoiceChip("${DemoData.OWNER_CALL_NAME}$it", selected = suffix == index) { suffix = index }
                }
            }
        }
    }
}

@Composable
private fun TitleStep() {
    val titles = listOf(DemoData.BOOK_TITLE, "길 위의 날들", "운전석에서 쓴 이야기")
    var title by rememberSaveable { mutableStateOf(titles.first()) }
    Column {
        Question("좋아요, ${DemoData.OWNER_CALL_NAME}님.\n이 자서전의 제목을\n정해 볼까요?")
        Column(Modifier.padding(top = 56.px), verticalArrangement = Arrangement.spacedBy(16.px)) {
            BasicText("자서전 제목", style = Label.style())
            InputField(title, onChange = { title = it })
            Row(horizontalArrangement = Arrangement.spacedBy(12.px)) {
                titles.forEach { ChoiceChip(it, selected = title == it) { title = it } }
            }
            BasicText("책등과 출판물 표지에 들어가요. 나중에 환경설정에서 바꿀 수 있어요.", style = Note.style(AutoColors.InkMuted))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChapterStep() {
    var chosen by rememberSaveable { mutableIntStateOf(2) }
    Column {
        Question("마지막으로,\n어떤 이야기부터\n시작할까요?")
        FlowRow(
            modifier = Modifier.padding(top = 56.px),
            horizontalArrangement = Arrangement.spacedBy(14.px),
            verticalArrangement = Arrangement.spacedBy(14.px),
        ) {
            DemoData.chapters.take(5).forEach { chapter ->
                ChapterChoice(chapter.number, chapter.title, selected = chosen == chapter.number) { chosen = chapter.number }
            }
            ChapterChoice(null, "오토가 골라 줘", selected = chosen == PICK_FOR_ME, dashed = true) { chosen = PICK_FOR_ME }
        }
        BasicText("12개 챕터는 어떤 순서로 채워도 괜찮아요.", style = Note.style(AutoColors.InkMuted), modifier = Modifier.padding(top = 18.px))
    }
}

/** 92px 알약 입력칸. 오른쪽 마이크는 말로 입력할 수 있다는 표시다. */
@Composable
private fun InputField(value: String, onChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.px)
            .border(2.5.px, AutoColors.Ink, RoundedCornerShape(percent = 50))
            .padding(horizontal = 34.px),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.px),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = Field.style(),
            cursorBrush = SolidColor(AutoColors.Ink),
            modifier = Modifier.weight(1f),
        )
        Image(rememberVectorPainter(AutoIcons.MicFilled), "말로 입력", Modifier.size(30.px), colorFilter = ColorFilter.tint(AutoColors.InkSecondary))
    }
}

@Composable
private fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = Modifier
            .height(68.px)
            .clip(shape)
            .then(if (selected) Modifier.background(AutoColors.Ink) else Modifier.border(2.5.px, AutoColors.Ink, shape))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 26.px),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = Chip.style(if (selected) AutoColors.OnInk else AutoColors.Ink), maxLines = 1)
    }
}

/** 첫 챕터 고르기(높이 96). 번호는 작게, 고르면 잉크로 채운다. ‘오토가 골라 줘’는 점선. */
@Composable
private fun ChapterChoice(number: Int?, title: String, selected: Boolean, dashed: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(percent = 50)
    val border = 2.5.px
    Row(
        modifier = Modifier
            .height(96.px)
            .clip(shape)
            .then(
                when {
                    selected -> Modifier.background(AutoColors.Ink)
                    dashed -> Modifier.drawBehind {
                        val w = border.toPx()
                        drawRoundRect(
                            color = AutoColors.Ink,
                            topLeft = Offset(w / 2, w / 2),
                            size = Size(size.width - w, size.height - w),
                            cornerRadius = CornerRadius((size.height - w) / 2),
                            style = Stroke(width = w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * 3, w * 3))),
                        )
                    }
                    else -> Modifier.border(border, AutoColors.Ink, shape)
                },
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 30.px),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val text = buildAnnotatedString {
            if (number != null) {
                withStyle(ChapterNumber.style(if (selected) AutoColors.LavenderSoft else AutoColors.InkSecondary).toSpanStyle()) {
                    append("%02d".format(number))
                }
                append("  ")
            }
            append(title)
        }
        BasicText(text, style = ChapterChip.style(if (selected) AutoColors.OnInk else AutoColors.Ink), maxLines = 1)
    }
}

/** 이전·다음(240×96, 명조). */
@Composable
private fun NavButton(text: String, filled: Boolean, onClick: () -> Unit, enabled: Boolean = true) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = Modifier
            .width(240.px)
            .height(96.px)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(shape)
            .then(if (filled) Modifier.background(AutoColors.Ink) else Modifier.border(2.5.px, AutoColors.Ink, shape))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = Nav.style(if (filled) AutoColors.OnInk else AutoColors.Ink))
    }
}

/** 마이크 권한. 허용했으면 ‘허용됨’, 아니면 눌러서 요청한다. */
@Composable
private fun MicPermissionRow(modifier: Modifier) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    Row(
        modifier = modifier.then(
            if (granted) Modifier else Modifier.clickable(role = Role.Button) { launcher.launch(Manifest.permission.RECORD_AUDIO) },
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.px),
    ) {
        Image(rememberVectorPainter(AutoIcons.MicFilled), null, Modifier.size(30.px), colorFilter = ColorFilter.tint(AutoColors.Ink))
        BasicText("마이크 사용 허용", style = TextSpec(24f, FontWeight.Bold).style())
        StatusChip(if (granted) "허용됨" else "허용하기", filled = granted, sizePx = 20f)
    }
}
