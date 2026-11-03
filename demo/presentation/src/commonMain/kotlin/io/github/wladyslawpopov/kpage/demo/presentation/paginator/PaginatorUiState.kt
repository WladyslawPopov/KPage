package io.github.wladyslawpopov.kpage.demo.presentation.paginator

import io.github.wladyslawpopov.kpage.core.paging.data.effect.PagingEvents
import io.github.wladyslawpopov.kpage.core.paging.data.state.LoadState
import io.github.wladyslawpopov.kpage.core.paging.data.state.PagingRow
import io.github.wladyslawpopov.kpage.core.paging.data.state.PagingState
import io.github.wladyslawpopov.kpage.core.paging.data.state.pagedListOf

/**
 * A paged list of a screen. [query] is what the list shows (a search, a dialog id); a new query gets
 * a new paginator. [D] are the decoration rows (`Nothing` without decorations); [rows] are the items
 * with them. [events] go to the list UI (PagingEffect or PagingListTracker).
 */
data class PaginatorUiState<Q : Any, T : Any, D : Any>(
    val query: Q,
    val paging: PagingState<T> = emptyPaging(),
    val rows: List<PagingRow<T, D>> = emptyList(),
    val events: PagingEvents? = null,
) {
    val items get() = paging.items
    val totalCount get() = paging.totalCount
    val loadState get() = paging.loadState
    val isEmptyResult: Boolean get() = loadState is LoadState.END && totalCount == 0
}

internal fun <T : Any> emptyPaging() = PagingState<T>(
    items = pagedListOf(0),
    totalCount = 0,
    loadState = LoadState.INITIAL,
    isRestored = false,
)
