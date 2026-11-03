package io.github.wladyslawpopov.kpage.demo.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.wladyslawpopov.kpage.demo.android.theme.color
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.style
import io.github.wladyslawpopov.kpage.demo.android.ui.widgets.TopBar
import io.github.wladyslawpopov.kpage.demo.shared.strings.asString
import io.github.wladyslawpopov.kpage.demo.uicore.model.TopBarState
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppTypographyKey

/** Layout of the empty screens of the skeleton. */
@Composable
fun PlaceholderScreen(topBar: TopBarState, text: AppStringKey) {
    Column(Modifier.fillMaxSize().background(AppColorKey.Container.BACKGROUND.color())) {
        // The skeleton screens are tab roots: the back button is never shown.
        TopBar(topBar, onBack = {})
        Box(
            Modifier.fillMaxSize().padding(AppDimenKey.Padding.LARGE.dp()),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text.asString(),
                style = AppTypographyKey.HEADLINE.style(),
                color = AppColorKey.Text.PRIMARY.color(),
            )
        }
    }
}
