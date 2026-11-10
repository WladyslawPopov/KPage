package io.github.wladyslawpopov.kpage.demo.presentation.screens.favorites

import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceImage
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageScreenState
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageState
import io.github.wladyslawpopov.kpage.demo.presentation.paginator.PaginatorUiState
import io.github.wladyslawpopov.kpage.demo.uicore.model.TopBarState
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.text.UiText

/**
 * @property selected The ids chosen for a removal; the screen is in the selection mode while there are any.
 * @property pendingDelete The ids waiting for the user's answer; the confirmation is shown while there are any.
 */
data class FavoritesState(
    override val page: PageState = PageState(),
    val topBar: TopBarState = TopBarState(AppStringKey.Tabs.FAVORITES),
    val list: PaginatorUiState<Unit, SpaceImage, Nothing> = PaginatorUiState(Unit),
    val selected: Set<String> = emptySet(),
    val pendingDelete: Set<String> = emptySet(),
) : PageScreenState<FavoritesState> {
    val isSelecting: Boolean
        get() = selected.isNotEmpty()

    /** The title of the bar in the selection mode: "Selected 3". */
    val selectionTitle: UiText
        get() = UiText.KeyWith(AppStringKey.Favorites.SELECTED, selected.size.toString())

    override fun withPage(page: PageState) = copy(page = page)
}
