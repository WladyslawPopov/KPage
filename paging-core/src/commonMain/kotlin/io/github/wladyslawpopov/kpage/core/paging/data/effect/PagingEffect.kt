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
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.wladyslawpopov.kpage.core.paging.common.rememberConfigurationChangeCheck
import io.github.wladyslawpopov.kpage.core.paging.data.NOT_LAID_OUT
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

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
    val visibility = remember(events) { Visibility() }

    // Observe only the visible position, never scan the list or decide what to fetch in the UI.
    LaunchedEffect(state, events) {
        snapshotFlow {
            val rows = currentItems
            val visible = visibleRows()
            // Before the first layout a non-empty list has no visible rows yet: nothing to report.
            if (visible.isEmpty() && rows.isNotEmpty()) return@snapshotFlow NOT_LAID_OUT
            val indexes = visible.mapNotNull {
                if (it in rows.indices) sourceIndex(it, rows[it]) else null
            }
            // Null: no server rows are visible (an empty list, or only decorations).
            if (indexes.isEmpty()) null else indexes.min()..indexes.max()
        }.filter { it !== NOT_LAID_OUT }
            .distinctUntilChanged()
            .collect { range ->
                visibility.known = true
                visibility.lastRange = range
                // A list shown without rows is shown again once it gets them: it may only have been
                // not read yet, and the paginator decides about the refresh by the first rows.
                val show = !visibility.shown || (range != null && !visibility.withRows)
                if (show && lifecycleOwner.isAtLeastStarted) {
                    visibility.shown = true
                    visibility.withRows = range != null
                    events.onShown(range)
                } else if (range != null) {
                    events.onVisibleRangeChanged(range)
                }
            }
    }

    DisposableEffect(lifecycleOwner, events) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (!visibility.shown && visibility.known) {
                    visibility.shown = true
                    visibility.withRows = visibility.lastRange != null
                    events.onShown(visibility.lastRange)
                }

                Lifecycle.Event.ON_STOP -> if (visibility.shown && !isConfigurationChange()) {
                    visibility.shown = false
                    events.onHidden()
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (visibility.shown && !isConfigurationChange()) {
                visibility.shown = false
                events.onHidden()
            }
        }
    }
}

private class Visibility {
    // onShown was sent since the last onHidden.
    var shown = false
    // The last onShown carried visible rows.
    var withRows = false
    // The visible rows were observed at least once; lastRange is meaningful.
    var known = false
    var lastRange: IntRange? = null
}

internal fun sourceIndex(uiIndex: Int, item: Any?): Int? =
    (if (item is PagingIndexedItem) item.serverIndex else uiIndex)?.takeIf { it >= 0 }

private val LifecycleOwner.isAtLeastStarted: Boolean
    get() = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
