package com.pleosautography.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pleosautography.app.ui.components.PillButton
import com.pleosautography.app.ui.components.RuledText
import com.pleosautography.app.ui.icons.AutoIcons
import com.pleosautography.app.ui.navigation.Screen
import com.pleosautography.app.ui.theme.AutoColors
import com.pleosautography.app.ui.theme.AutoType
import com.pleosautography.app.ui.theme.px

/** 아직 만들지 않은 화면의 자리. 마일스톤 2에서 시안대로 바뀐다. */
@Composable
fun PlaceholderScreen(screen: Screen, onBack: (() -> Unit)?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.px),
        verticalArrangement = Arrangement.spacedBy(32.px),
    ) {
        if (onBack != null) {
            PillButton("뒤로", onBack, icon = AutoIcons.ChevronLeft, heightPx = 72f, iconSizePx = 24f)
        }
        RuledText(screen.label, AutoType.PlaceholderTitle, Modifier.fillMaxWidth())
        BasicText(
            "마일스톤 2에서 design/screens/${screen.name}.png 시안대로 만듭니다.",
            style = AutoType.Meta.style(AutoColors.InkMuted),
        )
    }
}
