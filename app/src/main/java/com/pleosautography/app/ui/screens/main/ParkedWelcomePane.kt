package com.pleosautography.app.ui.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.pleosautography.app.ui.components.RuledText
import com.pleosautography.app.ui.theme.AutoColors
import com.pleosautography.app.ui.theme.GowunBatang
import com.pleosautography.app.ui.theme.TextSpec
import com.pleosautography.app.ui.theme.px

private val Greeting = TextSpec(56f, FontWeight.Bold, GowunBatang, lineHeightPx = 80f, letterSpacingEm = -0.015f)
private val Hint = TextSpec(24f, FontWeight.SemiBold)

/**
 * 가로 전체 창 정차 중 오른쪽 칸에 아무 화면도 열려 있지 않을 때. 시안: design/screens/FullPark.png
 * 인사말을 큰 명조·노트 줄·오른쪽 정렬로 쓴다.
 */
@Composable
fun ParkedWelcomePane(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 56.px, top = 36.px, end = 56.px, bottom = 40.px),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.End,
    ) {
        RuledText(
            text = parkedGreeting(singleLine = false),
            spec = Greeting,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth(),
        )
        BasicText(
            "왼쪽에서 챕터를 고르면 여기에 열려요",
            style = Hint.style(AutoColors.InkMuted),
            modifier = Modifier.padding(top = 28.px),
        )
    }
}
