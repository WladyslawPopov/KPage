package io.github.wladyslawpopov.kpage.core.paging.data.state

/**
 * Everything the UI needs to draw a list, delivered together.
 *
 * [items] and [totalCount] always come from one database snapshot, so they never disagree:
 * `items.size == totalCount`.
 *
 * @property items The list with a slot for every server position; see [PagedList].
 * @property totalCount Total number of items on the server, restored from the cache on a cold start.
 * @property loadState Network state of the paginator.
 * @property isRestored False until the first snapshot has been read from the database; before that
 * an empty list means "not read yet", not "the list is empty".
 */
data class PagingState<T : Any>(
    val items: PagedList<T>,
    val totalCount: Int,
    val loadState: LoadState,
    val isRestored: Boolean,
) {
    companion object {
        /**
         * The state before the paginator has delivered anything: no items, the first data is on its way
         * ([LoadState.INITIAL]), nothing read from the database yet. A paginator starts with it, and a screen
         * that mirrors a paginator can use it as its own initial value.
         */
        fun <T : Any> initial(): PagingState<T> =
            PagingState(PagedList.empty(), totalCount = 0, loadState = LoadState.INITIAL, isRestored = false)
    }
}
