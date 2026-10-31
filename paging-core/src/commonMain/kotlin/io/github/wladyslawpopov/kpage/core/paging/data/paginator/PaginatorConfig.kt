package io.github.wladyslawpopov.kpage.core.paging.data.paginator

import io.github.wladyslawpopov.kpage.core.paging.data.PAGE_SIZE

/**
 * @property pageSize Number of items per page.
 * @property prefetchDistance Pages of the window on each side of the anchor page
 * (the window is `2 * prefetchDistance + 1` pages).
 * @property initialPageKey Number of the first page; it is added to the page index passed to `getPage`.
 * @property extraPagesInDb Spare pages kept in the cache beyond the window, nearest to it first.
 */
data class PaginatorConfig(
    val pageSize: Int = PAGE_SIZE,
    val prefetchDistance: Int = 1,
    val initialPageKey: Int = 0,
    val extraPagesInDb: Int = 2,
) {
    init {
        require(pageSize >= 1) { "pageSize must be at least 1" }
        require(prefetchDistance >= 0) { "prefetchDistance must not be negative" }
        require(extraPagesInDb >= 0) { "extraPagesInDb must not be negative" }
        require(initialPageKey >= 0) { "initialPageKey must not be negative" }
    }
}
