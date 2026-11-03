package io.github.wladyslawpopov.kpage.demo.presentation.screens.chat

import io.github.wladyslawpopov.kpage.demo.presentation.page.PageScreenState
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageState
import io.github.wladyslawpopov.kpage.demo.uicore.model.TopBarState
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey

data class ChatState(
    override val page: PageState = PageState(),
    val topBar: TopBarState = TopBarState(AppStringKey.Tabs.CHAT),
    val text: AppStringKey = AppStringKey.Placeholder.CHAT,
) : PageScreenState<ChatState> {
    override fun withPage(page: PageState) = copy(page = page)
}

sealed interface ChatIntent
