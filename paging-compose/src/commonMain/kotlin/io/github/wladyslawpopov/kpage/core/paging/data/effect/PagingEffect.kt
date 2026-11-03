package io.github.wladyslawpopov.kpage.core.paging.data.effect

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.wladyslawpopov.kpage.core.paging.common.rememberConfigurationChangeCheck

/**
 * Connects the app's lazy list to the paginator. It reports the server indexes of the visible items
 * and whether the list is shown or hidden (leaving the composition or stopping the host).
 * A configuration change is not reported as leaving. A new [events] object gets its own `onShown`.
 *
 * The list itself (layout, keys, placeholders, decorations, scrollbars) belongs to the app.
 *
 * @param items Exactly the rows of the lazy list, in the same order: row `i` of the list is
 * `items[i]`. Rows implementing [PagingIndexedItem] give their server index (decorations return
 * null); other rows use their position in [items]. A row added to the lazy list outside [items]
 * (for example `item { Header() }`) shifts the indexes; add such rows with `decorate` instead.
 *
 * Use keys that include the server position (for example `"${item.id}_$serverIndex"`), not the id
 * alone: after the server shifts its pages, the same item can briefly sit on two positions, and
 * duplicate keys crash a lazy list.
 */
@Composable
fun PagingEffect(state: LazyListState, items: List<Any?>, events: PagingEvents) =
    PagingEffect(state, items, events) { state.layoutInfo.visibleItemsInfo.map { it.index } }

/** [PagingEffect] for a lazy grid. */
@Composable
fun PagingEffect(state: LazyGridState, items: List<Any?>, events: PagingEvents) =
    PagingEffect(state, items, events) { state.layoutInfo.visibleItemsInfo.map { it.index } }

/** [PagingEffect] for a lazy staggered grid. */
@Composable
fun PagingEffect(state: LazyStaggeredGridState, items: List<Any?>, events: PagingEvents) =
    PagingEffect(state, items, events) { state.layoutInfo.visibleItemsInfo.map { it.index } }

@Composable
private fun PagingEffect(
    state: Any,
    items: List<Any?>,
    events: PagingEvents,
    visibleRows: () -> List<Int>,
) {
    val currentItems by rememberUpdatedState(items)
    val lifecycleOwner = LocalLifecycleOwner.current
    val isConfigurationChange = rememberConfigurationChangeCheck()
    // A new paginator (for example after a filter change) shows a new list and gets its own onShown.
    val tracker = remember(events) { PagingListTracker(events) }

    // Observe only the visible position, never scan the list or decide what to fetch in the UI.
    LaunchedEffect(state, tracker) {
        snapshotFlow { Layout(currentItems, visibleRows()) }
            .collect { tracker.onLayout(it.rows, it.visibleRows) }
    }

    DisposableEffect(lifecycleOwner, tracker) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> tracker.onStart()
                Lifecycle.Event.ON_STOP -> tracker.onStop(isConfigurationChange())
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            tracker.onStop(isConfigurationChange())
        }
    }
}

// Not a data class: snapshotFlow would compare the rows with equals on every new data snapshot,
// which for a decorated or custom list can walk all totalCount rows. The tracker skips repeated
// ranges itself.
private class Layout(val rows: List<Any?>, val visibleRows: List<Int>)
