package io.github.wladyslawpopov.kpage.demo.presentation.paginator

import io.github.wladyslawpopov.kpage.core.paging.data.paginator.Paginator
import io.github.wladyslawpopov.kpage.core.paging.data.state.DecorationScope
import io.github.wladyslawpopov.kpage.core.paging.data.state.LoadState
import io.github.wladyslawpopov.kpage.core.paging.data.state.PagingRows
import io.github.wladyslawpopov.kpage.core.paging.data.state.PagingState
import io.github.wladyslawpopov.kpage.core.paging.data.state.decorate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * Owns the paginator of a paged list: one per query, mirrored into [PaginatorUiState], load errors
 * reported once. The screen keeps the state; the engine reads and writes it through the lambdas.
 *
 * @param refreshOn Every event renews the list: the app's own reasons to ask for a refresh that come
 * from outside the screen, for example a cleared cache that left the list empty.
 */
class PaginatorEngine<Q : Any, T : Any, D : Any>(
    private val getState: () -> PaginatorUiState<Q, T, D>,
    private val updateState: (PaginatorUiState<Q, T, D>) -> Unit,
    private val onQueryChanged: () -> Unit = {},
    private val onError: (Throwable) -> Unit = {},
    private val refreshOn: Flow<Unit> = emptyFlow(),
) {
    private var paginator: Paginator<T>? = null
    private var lastError: Throwable? = null
    private var decorations: DecorationScope<T, D>.() -> Unit = {}

    fun onIntent(intent: PaginatorIntent<Q>) {
        when (intent) {
            is PaginatorIntent.SetQuery -> setQuery(intent.query)
        }
    }

    /** Renews the list and returns when the paginator has finished; the state is refreshing meanwhile. */
    suspend fun refresh() {
        val paginator = paginator ?: return
        updateState(getState().copy(isRefreshing = true))
        try {
            paginator.refresh()
        } finally {
            // A new query has its own paginator, already shown as not refreshing.
            if (this.paginator === paginator) updateState(getState().copy(isRefreshing = false))
        }
    }

    /**
     * Runs a paginator per query until the caller is cancelled. Each paginator lives in a scope that a
     * new query or the caller's cancellation ends.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun collectPaging(queries: Flow<Q>, create: (Q, CoroutineScope) -> Paginator<T>) {
        coroutineScope {
            launch { refreshOn.collect { refresh() } }
            queries
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    flow {
                        coroutineScope {
                            val newPaginator = create(query, this)
                            setPaginator(newPaginator)
                            emitAll(newPaginator.state)
                        }
                    }
                }
                .collect { paging ->
                    updateState(
                        getState().copy(
                            paging = paging,
                            rows = paging.items.decorate(decorations),
                        )
                    )
                    reportError(paging.loadState)
                }
        }
    }

    /**
     * Declares the decoration rows. The block is kept and applied to every new snapshot; call it again
     * when the data it reads changes.
     */
    fun decorate(block: DecorationScope<T, D>.() -> Unit) {
        decorations = block
        val state = getState()
        updateState(state.copy(rows = state.items.decorate(block)))
    }

    suspend fun updateItem(id: String, transform: (T) -> T): T? =
        paginator?.updateItem(id, transform)

    suspend fun getItem(id: String): T? = paginator?.getItemById(id)

    private fun setPaginator(newPaginator: Paginator<T>) {
        paginator?.close()
        paginator = newPaginator
        lastError = null
        updateState(
            getState().copy(
                events = newPaginator,
                paging = PagingState.initial(),
                rows = PagingRows.empty(),
                isRefreshing = false,
            )
        )
    }

    private fun setQuery(query: Q) {
        val state = getState()
        if (state.query == query) return
        onQueryChanged()
        updateState(state.copy(query = query))
    }

    private fun reportError(loadState: LoadState) {
        val error = (loadState as? LoadState.ERROR)?.error
        if (error == null) {
            lastError = null
        } else if (lastError !== error) {
            lastError = error
            onError(error)
        }
    }
}
