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
 * Connects the app's lazy list to the paginator. It reports the server positions of the visible rows
 * and whether the list is shown or hidden (leaving the composition or stopping the host).
 * A configuration change is not reported as leaving. A new [events] object gets its own `onShown`.
 *
 * The list itself (layout, keys, placeholders, decorations, scrollbars) belongs to the app.
 *
 * @param rows Exactly the rows of the lazy list, in the same order: row `i` of the list is row `i` of
 * [rows]. Usually `PagingRows` from `decorate` or `rows()`, read with
 * `items(count = rows.rowCount, key = rows.keys { … }) { rows[it] }`. A row added to the lazy list
 * outside [rows] (for example `item { Header() }`) shifts the positions; add such rows with `decorate`.
 */
@Composable
fun PagingEffect(state: LazyListState, rows: PagingRowSource, events: PagingEvents) =
    PagingEffect(state, rows, events) { state.layoutInfo.visibleItemsInfo.map { it.index } }

/** [PagingEffect] for a lazy grid. */
@Composable
fun PagingEffect(state: LazyGridState, rows: PagingRowSource, events: PagingEvents) =
    PagingEffect(state, rows, events) { state.layoutInfo.visibleItemsInfo.map { it.index } }

/** [PagingEffect] for a lazy staggered grid. */
@Composable
fun PagingEffect(state: LazyStaggeredGridState, rows: PagingRowSource, events: PagingEvents) =
    PagingEffect(state, rows, events) { state.layoutInfo.visibleItemsInfo.map { it.index } }

/**
 * [PagingEffect] for a lazy list of any rows. Rows implementing [PagingIndexedItem] give their server
 * index (decorations return null); other rows use their position in [items].
 */
@Composable
fun PagingEffect(state: LazyListState, items: List<Any?>, events: PagingEvents) =
    PagingEffect(state, rememberRowSource(items), events)

/** [PagingEffect] for a lazy grid of any rows. */
@Composable
fun PagingEffect(state: LazyGridState, items: List<Any?>, events: PagingEvents) =
    PagingEffect(state, rememberRowSource(items), events)

/** [PagingEffect] for a lazy staggered grid of any rows. */
@Composable
fun PagingEffect(state: LazyStaggeredGridState, items: List<Any?>, events: PagingEvents) =
    PagingEffect(state, rememberRowSource(items), events)

// A new source only for a new list: the effect reports again only when the rows really change.
@Composable
private fun rememberRowSource(items: List<Any?>): PagingRowSource = remember(items) { PagingRowSource.of(items) }

@Composable
private fun PagingEffect(
    state: Any,
    rows: PagingRowSource,
    events: PagingEvents,
    visibleRows: () -> List<Int>,
) {
    val currentRows by rememberUpdatedState(rows)
    val lifecycleOwner = LocalLifecycleOwner.current
    val isConfigurationChange = rememberConfigurationChangeCheck()
    // A new paginator (for example after a filter change) shows a new list and gets its own onShown.
    val tracker = remember(events) { PagingListTracker(events) }

    // Observe only the visible position, never scan the list or decide what to fetch in the UI.
    LaunchedEffect(state, tracker) {
        snapshotFlow { Layout(currentRows, visibleRows()) }
            .collect { tracker.onLayout(it.rows, it.visibleRows) }
    }

    // A configuration change recreates the composition and its tracker: the old one is not stopped,
    // so the paginator never hears that the list left and does not refresh after a rotation.
    DisposableEffect(lifecycleOwner, tracker) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> tracker.onStart()
                Lifecycle.Event.ON_STOP -> if (!isConfigurationChange()) tracker.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (!isConfigurationChange()) tracker.onStop()
        }
    }
}

// Not a data class: snapshotFlow would compare the rows with equals on every new data snapshot.
// The tracker skips repeated ranges itself.
private class Layout(val rows: PagingRowSource, val visibleRows: List<Int>)
