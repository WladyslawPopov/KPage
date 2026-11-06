package io.github.wladyslawpopov.kpage.demo.presentation.screens.space

import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceCenter
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceImage
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceQuery
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageScreenState
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageState
import io.github.wladyslawpopov.kpage.demo.presentation.paginator.PaginatorUiState
import io.github.wladyslawpopov.kpage.demo.uicore.model.TopBarState
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey

/**
 * @property searchText What is typed in the search field. The list follows it after a pause, so
 * [PaginatorUiState.query] of [list] is what the list shows now: the trimmed text and the filters.
 */
data class SpaceState(
    override val page: PageState = PageState(),
    val topBar: TopBarState = TopBarState(AppStringKey.Tabs.SPACE),
    val searchText: String = "",
    val layout: SpaceLayout = SpaceLayout.LIST,
    val list: PaginatorUiState<SpaceQuery, SpaceImage, Nothing> = PaginatorUiState(SpaceQuery()),
) : PageScreenState<SpaceState> {
    val isFiltered: Boolean
        get() = with(list.query) { center != null || yearStart != null || yearEnd != null }

    override fun withPage(page: PageState) = copy(page = page)
}

enum class SpaceLayout { LIST, GRID }

sealed interface SpaceIntent {
    /** The text of the search field changed. */
    data class SearchChanged(val text: String) : SpaceIntent

    /** Sets all the filters at once; null clears a filter. The list changes at once. */
    data class SetFilters(
        val center: SpaceCenter?,
        val yearStart: Int?,
        val yearEnd: Int?,
    ) : SpaceIntent

    data object ToggleLayout : SpaceIntent

    /** The heart of an image. The value is sent, not toggled, so a repeated tap cannot flip it twice. */
    data class SetFavorite(val id: String, val isFavorite: Boolean) : SpaceIntent
}
