package io.github.wladyslawpopov.kpage.demo.presentation.screens.settings

import io.github.wladyslawpopov.kpage.demo.presentation.page.PageScreenState
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageState
import io.github.wladyslawpopov.kpage.demo.uicore.model.TopBarState
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey

data class SettingsState(
    override val page: PageState = PageState(),
    val topBar: TopBarState = TopBarState(AppStringKey.Tabs.SETTINGS),
    val text: AppStringKey = AppStringKey.Placeholder.SETTINGS,
) : PageScreenState<SettingsState> {
    override fun withPage(page: PageState) = copy(page = page)
}

sealed interface SettingsIntent
