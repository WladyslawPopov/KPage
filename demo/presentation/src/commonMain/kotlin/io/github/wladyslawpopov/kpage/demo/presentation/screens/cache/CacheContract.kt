package io.github.wladyslawpopov.kpage.demo.presentation.screens.cache

import io.github.wladyslawpopov.kpage.demo.presentation.page.PageScreenState
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageState
import io.github.wladyslawpopov.kpage.demo.uicore.model.TopBarState
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey

data class CacheState(
    override val page: PageState = PageState(),
    val topBar: TopBarState = TopBarState(AppStringKey.Tabs.CACHE),
    val text: AppStringKey = AppStringKey.Placeholder.CACHE,
) : PageScreenState<CacheState> {
    override fun withPage(page: PageState) = copy(page = page)
}

sealed interface CacheIntent
