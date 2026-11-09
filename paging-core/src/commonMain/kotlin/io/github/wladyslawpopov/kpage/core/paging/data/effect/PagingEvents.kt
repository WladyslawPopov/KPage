package io.github.wladyslawpopov.kpage.core.paging.data.effect

/**
 * What the list UI tells the paginator: where it is scrolled. `visible` holds the server positions
 * of the first and the last visible item; the paginator turns them into pages with its own page size,
 * so the UI does not need to know it. Whether the list is on screen or not is not reported: the app
 * decides when to renew the data (see `Paginator.refresh`).
 */
interface PagingEvents {
    /** The visible items changed. Repeated ranges within the same pages are cheap. */
    fun onVisibleRangeChanged(visible: IntRange)
}
