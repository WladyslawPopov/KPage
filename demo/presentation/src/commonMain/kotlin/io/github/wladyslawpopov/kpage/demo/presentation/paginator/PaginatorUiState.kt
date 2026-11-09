package io.github.wladyslawpopov.kpage.demo.presentation.paginator

import io.github.wladyslawpopov.kpage.core.paging.data.effect.PagingEvents
import io.github.wladyslawpopov.kpage.core.paging.data.state.LoadState
import io.github.wladyslawpopov.kpage.core.paging.data.state.PagingRows
import io.github.wladyslawpopov.kpage.core.paging.data.state.PagingState

/**
 * A paged list of a screen. [query] is what the list shows (a search, a dialog id); a new query gets
 * a new paginator. [D] are the decoration rows (`Nothing` without decorations); [rows] are the items
 * with them, read by index on every platform. [events] go to the list UI, which
 * reports the visible rows through a `PagingListTracker`.
 */
data class PaginatorUiState<Q : Any, T : Any, D : Any>(
    val query: Q,
    val paging: PagingState<T> = PagingState.initial(),
    val rows: PagingRows<T, D> = PagingRows.empty(),
    val events: PagingEvents? = null,
) {
    val items get() = paging.items
    val totalCount get() = paging.totalCount
    val loadState get() = paging.loadState
    val isEmptyResult: Boolean get() = loadState is LoadState.END && totalCount == 0
}
