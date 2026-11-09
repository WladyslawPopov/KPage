package io.github.wladyslawpopov.kpage.core.paging.data.effect

/**
 * Connects any list UI to [PagingEvents]: turns the visible rows into the server positions of the
 * first and the last visible item and reports them when they change. The library draws nothing and
 * has no UI module: every app connects its own list (Compose, SwiftUI, UIKit, Android Views) by calling
 * [onLayout] from the place where it learns what is on screen. Create one tracker per [events] object:
 * a new paginator gets a new tracker. Not thread-safe: call it from the main thread.
 */
class PagingListTracker(private val events: PagingEvents) {
    private var lastRange: IntRange? = null

    /**
     * Reports what is on screen. [rows] are exactly the rows of the list, in the same order: row `i`
     * is read as `rows.serverIndexAt(i)` (null for decorations). [visibleRows] are indexes in [rows]
     * of the rows on screen, in any order. Call it on every layout change and whenever [rows] change.
     * Repeated calls are cheap: only the visible rows are read, and an unchanged range is not sent again.
     */
    fun onLayout(rows: PagingRowSource, visibleRows: List<Int>) {
        // Before the first layout a non-empty list has no visible rows yet: nothing to report.
        if (visibleRows.isEmpty() && rows.rowCount > 0) return
        var first = Int.MAX_VALUE
        var last = Int.MIN_VALUE
        for (row in visibleRows) {
            val index = rows.serverIndexAt(row) ?: continue
            if (index < first) first = index
            if (index > last) last = index
        }
        // Null: no server rows are visible (an empty list, or only decorations): nothing to report.
        val range = if (first <= last) first..last else null
        if (range == lastRange) return
        lastRange = range
        if (range != null) events.onVisibleRangeChanged(range)
    }
}

internal fun sourceIndex(uiIndex: Int, item: Any?): Int? =
    (if (item is PagingIndexedItem) item.serverIndex else uiIndex)?.takeIf { it >= 0 }
