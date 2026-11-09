package io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing

import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceImage
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceLayout
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceQuery
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageScreenState
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageState
import io.github.wladyslawpopov.kpage.demo.presentation.paginator.PaginatorUiState
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.display.SpaceChip
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.display.SpaceFiltersDisplay
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.mapper.spaceChips
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.mapper.spaceFilters
import io.github.wladyslawpopov.kpage.demo.uicore.model.TopBarState
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey

/**
 * @property searchText What is typed in the search field. The list follows it after a pause, so
 * [PaginatorUiState.query] of [list] is what the list shows now: the trimmed text and the filters.
 * @property chips What is set, ready to draw under the header; they follow [list] and [searchText].
 * @property filters What the filters panel draws besides its fixed options.
 */
data class SpaceState(
    override val page: PageState = PageState(),
    val topBar: TopBarState = TopBarState(AppStringKey.Tabs.SPACE),
    val searchText: String = "",
    val layout: SpaceLayout = SpaceLayout.LIST,
    val list: PaginatorUiState<SpaceQuery, SpaceImage, Nothing> = PaginatorUiState(SpaceQuery()),
) : PageScreenState<SpaceState> {
    val isFiltered: Boolean
        get() = with(list.query) { center != null || yearStart != null || yearEnd != null || tag != null }

    val chips: List<SpaceChip>
        get() = spaceChips(list.query, searchText)

    val filters: SpaceFiltersDisplay
        get() = spaceFilters(list.query, list.totalCount, page.isLoading)

    override fun withPage(page: PageState) = copy(page = page)
}
