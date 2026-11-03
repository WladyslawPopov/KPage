package io.github.wladyslawpopov.kpage.demo.presentation.screens.space

import io.github.wladyslawpopov.kpage.demo.presentation.page.PageScreenState
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageState
import io.github.wladyslawpopov.kpage.demo.uicore.model.TopBarState
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey

data class SpaceState(
    override val page: PageState = PageState(),
    val topBar: TopBarState = TopBarState(AppStringKey.Tabs.SPACE),
    val text: AppStringKey = AppStringKey.Placeholder.SPACE,
) : PageScreenState<SpaceState> {
    override fun withPage(page: PageState) = copy(page = page)
}

sealed interface SpaceIntent
