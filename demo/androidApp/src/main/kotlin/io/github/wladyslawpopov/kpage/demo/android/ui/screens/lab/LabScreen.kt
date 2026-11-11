package io.github.wladyslawpopov.kpage.demo.android.ui.screens.lab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.wladyslawpopov.kpage.demo.android.ui.page.PageLayout
import io.github.wladyslawpopov.kpage.demo.android.ui.widgets.TopBar
import io.github.wladyslawpopov.kpage.demo.presentation.screens.lab.LabComponent

@Composable
fun LabScreen(component: LabComponent) {
    val state by component.state.collectAsState()
    PageLayout(
        page = state.page,
        onPageIntent = component::onPageIntent,
        // The tab root: the back button is never shown.
        header = { TopBar(state.topBar, onBack = {}) },
    ) {
        // The page shows "coming soon" as its empty state, so there is no content yet.
    }
}
