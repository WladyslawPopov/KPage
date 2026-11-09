package io.github.wladyslawpopov.kpage.demo.android.ui.paging

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import io.github.wladyslawpopov.kpage.core.paging.data.effect.PagingEvents
import io.github.wladyslawpopov.kpage.core.paging.data.effect.PagingListTracker
import io.github.wladyslawpopov.kpage.core.paging.data.effect.PagingRowSource

/**
 * Tells the paginator which rows of [state] are on screen, through a [PagingListTracker]; the paginator
 * alone decides what to load. [rows] are exactly the rows of the lazy grid, in the same order. A new
 * [events] object (a new paginator) gets a new tracker.
 */
@Composable
fun TrackVisibleRows(state: LazyGridState, rows: PagingRowSource, events: PagingEvents) {
    val currentRows by rememberUpdatedState(rows)
    val tracker = remember(events) { PagingListTracker(events) }
    LaunchedEffect(state, tracker) {
        snapshotFlow { VisibleRows(currentRows, state.layoutInfo.visibleItemsInfo.map { it.index }) }
            .collect { tracker.onLayout(it.rows, it.indexes) }
    }
}

// Not a data class: snapshotFlow would compare the rows with equals on every new data snapshot.
// The tracker skips repeated ranges itself.
private class VisibleRows(val rows: PagingRowSource, val indexes: List<Int>)
