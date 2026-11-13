package io.github.wladyslawpopov.kpage.core.paging.data.paginator

import io.github.wladyslawpopov.kpage.core.paging.data.effect.PagingEvents
import io.github.wladyslawpopov.kpage.core.paging.data.state.PagingState
import kotlinx.coroutines.flow.StateFlow

/**
 * A paginated list backed by the local cache.
 *
 * - Read the list: [state].
 * - Report the scroll of the UI (a `PagingListTracker` turns the visible rows into positions): [onVisibleRangeChanged]. The paginator keeps the pages around the visible ones loaded; without the
 *   events it shows what it has.
 * - Renew data from the server: [refresh], always where the user is. The paginator renews the list once
 *   when it is created and never by itself after that: when to renew (a pull, a retry, a change of
 *   the query) is the app's decision.
 *
 * One rule for the whole API: [state] tells what is going on with the list (its own loads included);
 * the calls of the app ([refresh], [updateItem], [getItemById]) suspend until they are done.
 * - Change one item without the server: [updateItem]; the app sees it at once in every list.
 * - Work with one item: [getItemById] (read), [updateItem] (write). Removing is a server operation
 *   followed by [refresh].
 */
interface Paginator<T : Any> : PagingEvents {
    /** The list, its total count and the load state; `items.size == totalCount`. */
    val state: StateFlow<PagingState<T>>

    /**
     * Reloads the whole window around the current anchor, ignoring the cache. The old data stays on
     * screen until every page of the window has arrived; then they are replaced in one transaction
     * and pages outside the window are dropped. On failure the cache and the position are left
     * untouched, so it is safe to call again. Use it for pull-to-refresh, the retry button and after
     * removing items on the server. To refresh from the top, scroll the list to the top first.
     *
     * Returns when the refresh is over: the new data is written, or it failed (the error is in
     * [state] as `LoadState.ERROR`, not thrown), or a scroll out of the window or [close] ended it.
     * A refresh asked while another runs replaces it; both calls return when the new one is over.
     * Keep a pull-to-refresh indicator until it returns; not to wait, launch it.
     */
    suspend fun refresh()

    /**
     * Reads the full item from the cache, for example when UI rows hold only a display model and a
     * server call or a details screen needs the whole item. Items are shared by all lists of this
     * item type. Returns null if it is not cached.
     */
    suspend fun getItemById(id: String): T?

    /**
     * Changes the cached item with [transform] in one step (read and write together), at every
     * position where it occurs, in every list of this item type. Call it after the server confirmed
     * the change. Returns the new item, or null if the item is not cached.
     */
    suspend fun updateItem(id: String, transform: (T) -> T): T?

    /**
     * Stops this paginator before its scope ends, for example when it is replaced by a new one after
     * the query changed. Not needed when the owner is destroyed: cancelling the scope stops it.
     */
    fun close()
}
