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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.wladyslawpopov.kpager.core.paging.data.PagingIndexedItem
import io.github.wladyslawpopov.kpager.core.paging.data.PAGE_SIZE
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

/**
 * A highly customizable Composable grid layout designed to seamlessly work with [StablePaginator].
 * It reports the currently visible server page; the paginator manages all loading decisions.
 *
 * @param items The flattened list of currently loaded items. Null values represent loading placeholders.
 * For lists containing decorative rows, implement [PagingIndexedItem] on each UI item.
 * @param totalCount The total number of items available on the server.
 * @param pageSize The number of items loaded per page.
 * @param columns The number of grid columns (default is 1 for a standard vertical list).
 * @param isReversingPaging Set to true if the list should start from the bottom (e.g., chat applications).
 * @param showItemsCounter If true, displays the custom [counterBar] component.
 * @param state The [LazyGridState] to control and observe scroll position.
 * @param contentPadding Padding applied around the entire grid content.
 * @param spanProvider An optional lambda to specify custom column spans for specific items.
 * @param keyExtractor A lambda to provide stable unique keys for the grid items to prevent unwanted recompositions.
 * @param onVisiblePageChanged Reports the current server page to the paginator.
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
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    spanProvider: ((item: T) -> GridItemSpan)? = null,
    keyExtractor: (index: Int, item: T) -> Any,
    onVisiblePageChanged: (pageIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    counterBar : @Composable (Int) -> Unit = {},
    content: @Composable (Int, T?) -> Unit
) {
    require(pageSize > 0) { "pageSize must be positive" }
    val align = remember(isReversingPaging) { if (isReversingPaging) Alignment.BottomStart else Alignment.TopStart }
    val currentTotal by rememberUpdatedState(totalCount)
    val currentItems by rememberUpdatedState(items)
    val currentVisiblePageChanged by rememberUpdatedState(onVisiblePageChanged)

    val currentIndex by remember(state) {
        derivedStateOf {
            val last = state.layoutInfo.visibleItemsInfo.mapNotNull {
                if (it.index in currentItems.indices) sourceIndex(it.index, currentItems[it.index]) else null
            }.maxOrNull()
            if (last == null || currentTotal <= 0) 0 else (last + 1).coerceIn(1, currentTotal)
        }
    }

    // Observe only the visible position, never scan the list or decide what to fetch in the UI.
    LaunchedEffect(state, pageSize) {
        snapshotFlow {
            state.layoutInfo.visibleItemsInfo.mapNotNull {
                if (it.index in currentItems.indices) sourceIndex(it.index, currentItems[it.index]) else null
            }.minOrNull()?.div(pageSize)
        }.filterNotNull().distinctUntilChanged().collect { page ->
            currentVisiblePageChanged(page)
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


internal fun sourceIndex(uiIndex: Int, item: Any?): Int? =
    (if (item is PagingIndexedItem) item.serverIndex else uiIndex)?.takeIf { it >= 0 }
