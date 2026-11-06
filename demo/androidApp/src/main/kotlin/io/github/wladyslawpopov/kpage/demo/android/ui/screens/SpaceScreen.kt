package io.github.wladyslawpopov.kpage.demo.android.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import io.github.wladyslawpopov.kpage.demo.android.theme.color
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.style
import io.github.wladyslawpopov.kpage.demo.android.ui.page.PageLayout
import io.github.wladyslawpopov.kpage.demo.android.ui.paging.PagedGrid
import io.github.wladyslawpopov.kpage.demo.android.ui.widgets.TopBar
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.SpaceComponent
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppTypographyKey

@Composable
fun SpaceScreen(component: SpaceComponent) {
    val state by component.state.collectAsState()
    PageLayout(
        page = state.page,
        onPageIntent = component::onPageIntent,
        // A tab root: the back button is never shown.
        header = { TopBar(state.topBar, onBack = {}) },
    ) { padding ->
        PagedGrid(
            list = state.list,
            keyOf = { it.id },
            contentPadding = padding,
            placeholderContent = { TitleRow(null) },
            itemContent = { image, _ -> TitleRow(image.title) },
        )
    }
}

// Temporary row to check the page and the paginator; replaced by the cards.
@Composable
private fun TitleRow(title: String?) {
    Text(
        text = title ?: "…",
        style = AppTypographyKey.BODY.style(),
        color = if (title == null) AppColorKey.Text.MUTED.color() else AppColorKey.Text.PRIMARY.color(),
        modifier = Modifier.fillMaxWidth().padding(AppDimenKey.Padding.LARGE.dp()),
    )
}
