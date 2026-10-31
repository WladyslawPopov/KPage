package io.github.wladyslawpopov.kpage.core.paging.data.paginator

/**
 * One page from the server.
 *
 * @property totalCount Total number of items on the server, or null if the API does not report it.
 * Without it the list is as long as its loaded part, the next page is requested when the user nears
 * the end, and the end is a page with [isMore] false or fewer items than the page size.
 * @property isMore Whether the server has items after this page.
 */
data class PagerPayload<T>(
    val totalCount: Int?,
    val isMore: Boolean,
    val objects: List<T>
)
