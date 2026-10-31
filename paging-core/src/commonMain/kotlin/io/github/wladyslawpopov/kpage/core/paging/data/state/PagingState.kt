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
)
