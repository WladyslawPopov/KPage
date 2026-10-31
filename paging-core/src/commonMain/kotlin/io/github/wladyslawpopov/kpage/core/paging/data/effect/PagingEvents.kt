package io.github.wladyslawpopov.kpage.core.paging.data.effect

/**
 * Events from the list UI. `visible` holds the server positions of the first and the last visible
 * item; the paginator turns them into pages with its own page size, so the UI does not need to know it.
 */
interface PagingEvents {
    /**
     * The list is shown (first time or after being hidden). [visible] is null when the list has no
     * rows yet: an empty list is then refreshed, a list that has rows in the cache waits for them.
     */
    fun onShown(visible: IntRange?)

    /** The list is hidden: the user left the screen or the app went to the background. */
    fun onHidden()

    /** The visible items changed. Repeated ranges within the same pages are cheap. */
    fun onVisibleRangeChanged(visible: IntRange)
}
