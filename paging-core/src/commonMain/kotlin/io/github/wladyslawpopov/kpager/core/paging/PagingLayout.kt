package io.github.wladyslawpopov.kpager.core.paging

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.wladyslawpopov.kpager.core.paging.data.PAGE_SIZE
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * A highly customizable Composable grid layout designed to seamlessly work with [StablePaginator].
 * It automatically detects when the user scrolls near the end of the list and triggers data prefetching.
 *
 * @param items The flattened list of currently loaded items. Null values represent loading placeholders.
 * @param totalCount The total number of items available on the server.
 * @param pageSize The number of items loaded per page.
 * @param columns The number of grid columns (default is 1 for a standard vertical list).
 * @param isReversingPaging Set to true if the list should start from the bottom (e.g., chat applications).
 * @param showItemsCounter If true, displays the custom [counterBar] component.
 * @param state The [LazyGridState] to control and observe scroll position.
 * @param contentPadding Padding applied around the entire grid content.
 * @param spanProvider An optional lambda to specify custom column spans for specific items.
 * @param keyExtractor A lambda to provide stable unique keys for the grid items to prevent unwanted recompositions.
 * @param onPrefetch Callback triggered when the layout determines the next page needs to be fetched.
 * @param modifier Custom modifiers to apply to the root Box.
 * @param counterBar A custom Composable to display the current scroll position/index.
 * @param content The Composable block defining how each item should be rendered.
 */
@Composable
fun <T : Any> PagingLayout(
    items: List<T?>,
    totalCount: Int,
    pageSize: Int = PAGE_SIZE,
    columns: Int = 1,
    isReversingPaging: Boolean = false,
    showItemsCounter: Boolean = true,
    isPlaceholder: ((item: T?) -> Boolean)? = null,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    spanProvider: ((item: T) -> GridItemSpan)? = null,
    keyExtractor: (index: Int, item: T) -> Any,
    onPrefetch: (pageIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    counterBar : @Composable (Int) -> Unit = {},
    content: @Composable (Int, T?) -> Unit
) {
    val align = remember { if (isReversingPaging) Alignment.BottomStart else Alignment.TopStart }

    // Calculates the current globally visible index for the counter UI
    val currentIndex by remember(items) {
        derivedStateOf {
            if (items.isEmpty() || totalCount <= 0) 0
            else {
                val layoutInfo = state.layoutInfo
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                (lastVisibleItem + 1).coerceIn(1, maxOf(1, totalCount))
            }
        }
    }

    LaunchedEffect(state, pageSize, totalCount, items) {
        snapshotFlow {
            val layoutInfo = state.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@snapshotFlow emptySet()

            val firstVisible = visibleItems.first().index
            val lastVisible = visibleItems.last().index

            val pagesToPrefetch = mutableSetOf<Int>()

            // 1. Instant prefetch if ANY item currently visible on screen is a placeholder
            for (visibleItem in visibleItems) {
                val idx = visibleItem.index
                val item = items.getOrNull(idx)
                if (item == null || (isPlaceholder != null && isPlaceholder(item))) {
                    pagesToPrefetch.add(idx / pageSize)
                }
            }

            // 2. Prefetch next page when reaching bottom 25% of current page (scrolling down)
            val lastItemInCurrentPage = lastVisible % pageSize
            val bottomThreshold = pageSize - (pageSize / 4)
            if (lastItemInCurrentPage >= bottomThreshold) {
                pagesToPrefetch.add(lastVisible / pageSize)
            }

            // 3. Prefetch previous page when reaching top 25% of current page (scrolling up)
            val firstItemInCurrentPage = firstVisible % pageSize
            val topThreshold = pageSize / 4
            if (firstItemInCurrentPage <= topThreshold) {
                val currentPage = firstVisible / pageSize
                if (currentPage > 0) {
                    pagesToPrefetch.add(currentPage - 1)
                }
            }

            pagesToPrefetch
        }
            .distinctUntilChanged()
            .collect { pages ->
                for (page in pages) {
                    println("[PagingGridLayout] Scroll threshold or placeholder hit -> onPrefetch(page = $page)")
                    onPrefetch(page)
                }
            }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyGridWithScrollBars(
            columns = columns,
            state = state,
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            reverseLayout = isReversingPaging,
            listModifier = Modifier.fillMaxSize().align(align).then(modifier)
        ) {
            itemsIndexed(
                items,
                key = { index, item ->
                    if (item != null) keyExtractor(index, item) else "placeholder_$index"
                },
                span = spanProvider?.let { provider ->
                    { _, item ->
                        if (item != null) provider(item) else GridItemSpan(1)
                    }
                }
            ) { index, item ->
                content(index, item)
            }
        }

        if (showItemsCounter) {
            counterBar(currentIndex)
        }
    }
}
