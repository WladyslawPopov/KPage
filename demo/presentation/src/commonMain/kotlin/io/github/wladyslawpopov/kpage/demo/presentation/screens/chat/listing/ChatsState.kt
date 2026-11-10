package io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.listing

import io.github.wladyslawpopov.kpage.demo.domain.chat.Chat
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageScreenState
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageState
import io.github.wladyslawpopov.kpage.demo.presentation.paginator.PaginatorUiState
import io.github.wladyslawpopov.kpage.demo.uicore.model.TopBarState
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.text.UiText

/**
 * @property selected The ids chosen for a removal; the screen is in the selection mode while there are any.
 * @property pendingDelete The ids waiting for the user's answer; the confirmation is shown while there are any.
 * @property isCreating The dialog of a new chat is shown; its fields belong to the screen.
 */
data class ChatsState(
    override val page: PageState = PageState(),
    val topBar: TopBarState = TopBarState(AppStringKey.Tabs.CHAT),
    val list: PaginatorUiState<Unit, Chat, Nothing> = PaginatorUiState(Unit),
    val selected: Set<Long> = emptySet(),
    val pendingDelete: Set<Long> = emptySet(),
    val isCreating: Boolean = false,
) : PageScreenState<ChatsState> {
    val isSelecting: Boolean
        get() = selected.isNotEmpty()

    /** The title of the bar in the selection mode: "Selected 3". */
    val selectionTitle: UiText
        get() = UiText.KeyWith(AppStringKey.Chat.SELECTED, selected.size.toString())

    override fun withPage(page: PageState) = copy(page = page)
}
