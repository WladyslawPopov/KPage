package io.github.wladyslawpopov.kpage.core.paging.data.paginator

import io.github.wladyslawpopov.kpage.core.paging.data.effect.PagingEvents
import io.github.wladyslawpopov.kpage.core.paging.data.state.PagingState
import kotlinx.coroutines.flow.StateFlow

/**
 * A paginated list backed by the local cache.
 *
 * - Read the list: [state].
 * - Report the UI (usually done by `PagingEffect`): [onVisibleRangeChanged], [onShown], [onHidden].
 *   Without these events a list with a cache loads nothing and shows the cache as it is.
 * - Renew data from the server: [refresh], always where the user is.
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
     */
    fun refresh()

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
