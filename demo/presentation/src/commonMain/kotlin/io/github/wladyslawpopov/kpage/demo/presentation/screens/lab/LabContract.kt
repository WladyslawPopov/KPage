package io.github.wladyslawpopov.kpage.demo.presentation.screens.lab

import io.github.wladyslawpopov.kpage.demo.presentation.page.PageScreenState
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageState
import io.github.wladyslawpopov.kpage.demo.uicore.model.TopBarState
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey

data class LabState(
    override val page: PageState = PageState(),
    val topBar: TopBarState = TopBarState(AppStringKey.Tabs.LAB),
    val text: AppStringKey = AppStringKey.Placeholder.LAB,
) : PageScreenState<LabState> {
    override fun withPage(page: PageState) = copy(page = page)
}

sealed interface LabIntent
