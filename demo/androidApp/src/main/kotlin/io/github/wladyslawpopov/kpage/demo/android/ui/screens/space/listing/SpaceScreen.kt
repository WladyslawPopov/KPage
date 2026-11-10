package io.github.wladyslawpopov.kpage.demo.android.ui.screens.space.listing

import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.wladyslawpopov.kpage.demo.android.theme.vector
import io.github.wladyslawpopov.kpage.demo.android.ui.page.PageLayout
import io.github.wladyslawpopov.kpage.demo.android.ui.paging.PagedGrid
import io.github.wladyslawpopov.kpage.demo.android.ui.paging.rememberCanHideBars
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.space.common.Heart
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.space.listing.cards.ImageRow
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.space.listing.cards.ImageTile
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.space.listing.filters.SpaceFilterChips
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.space.listing.filters.SpaceFiltersSheet
import io.github.wladyslawpopov.kpage.demo.android.ui.widgets.BarIconButton
import io.github.wladyslawpopov.kpage.demo.android.ui.widgets.SearchTopBar
import io.github.wladyslawpopov.kpage.demo.android.ui.widgets.rememberBackAction
import io.github.wladyslawpopov.kpage.demo.android.ui.widgets.rememberSearchBarState
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceImage
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceLayout
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.SpaceComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.SpaceIntent
import io.github.wladyslawpopov.kpage.demo.shared.strings.asString
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey

@Composable
fun SpaceScreen(component: SpaceComponent) {
    val state by component.state.collectAsState()
    val isGrid = state.layout == SpaceLayout.GRID
    val onFavorite = { image: SpaceImage ->
        component.onIntent(SpaceIntent.SetFavorite(image.id, !image.isFavorite))
    }
    val onOpen = { image: SpaceImage -> component.onIntent(SpaceIntent.OpenImage(image.id)) }
    val gridState = rememberLazyGridState()
    val canHideBars by rememberCanHideBars(gridState)
    var isFiltersShown by rememberSaveable { mutableStateOf(false) }
    val searchBar = rememberSearchBarState()
    if (isFiltersShown) {
        SpaceFiltersSheet(state = state, onIntent = component::onIntent, onDismiss = { isFiltersShown = false })
    }
    PageLayout(
        page = state.page,
        onPageIntent = component::onPageIntent,
        hideBottomBarOnScroll = canHideBars,
        header = {
            SearchTopBar(
                state = state.topBar,
                searchBar = searchBar,
                text = state.searchText,
                hint = AppStringKey.Space.SEARCH_HINT,
                onTextChange = { component.onIntent(SpaceIntent.SearchChanged(it)) },
                onBack = rememberBackAction(),
            ) {
                BarIconButton(onClick = { isFiltersShown = true }) {
                    Icon(AppIconKey.FILTER.vector(), contentDescription = AppStringKey.Core.FILTERS.asString())
                }
                // Shows the layout a tap switches to.
                BarIconButton(onClick = { component.onIntent(SpaceIntent.ToggleLayout) }) {
                    Icon((if (isGrid) AppIconKey.LAYOUT_LIST else AppIconKey.LAYOUT_GRID).vector(), contentDescription = null)
                }
            }
            SpaceFilterChips(
                chips = state.chips,
                isSearchOpen = searchBar.isOpen,
                onOpenSearch = searchBar::open,
                onOpenFilters = { isFiltersShown = true },
                onIntent = component::onIntent,
            )
        },
    ) { padding ->
        PagedGrid(
            list = state.list,
            keyOf = { it.id },
            columns = if (isGrid) GRID_COLUMNS else 1,
            gridState = gridState,
            contentPadding = padding,
            // List rows keep their own padding and a divider.
            spacing = if (isGrid) AppDimenKey.Padding.MEDIUM else AppDimenKey.Padding.NONE,
            placeholderContent = { if (isGrid) ImageTile(null, {}, {}) else ImageRow(null, {}) },
            itemContent = { image, _ ->
                if (isGrid) {
                    ImageTile(image, onClick = { onOpen(image) }, onFavorite = { onFavorite(image) })
                } else {
                    ImageRow(image, onClick = { onOpen(image) }) { Heart(it.isFavorite, onClick = { onFavorite(image) }) }
                }
            },
        )
    }
}

private const val GRID_COLUMNS = 2
