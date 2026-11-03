package io.github.wladyslawpopov.kpage.core.paging.data.effect

/**
 * Connects any list UI to [PagingEvents]: decides when the list is shown, hidden or scrolled.
 * Compose uses it through `PagingEffect` (paging-compose); other UIs (SwiftUI, UIKit, Android Views)
 * call it directly. Create one tracker per [events] object: a new paginator gets a new tracker and
 * its own `onShown`. Not thread-safe: call it from the main thread.
 */
class PagingListTracker(private val events: PagingEvents) {
    // The host is visible (between onStart and onStop).
    private var started = false
    // onShown was sent since the last onHidden.
    private var shown = false
    // The last onShown carried visible rows.
    private var withRows = false
    // The visible rows were reported at least once; lastRange is meaningful.
    private var known = false
    private var lastRange: IntRange? = null

    /**
     * Reports what is on screen. [rows] are exactly the rows of the list, in the same order: row `i`
     * is `rows[i]`. Rows implementing [PagingIndexedItem] give their server index (decorations return
     * null); other rows use their position. [visibleRows] are indexes in [rows] of the rows on
     * screen, in any order. Call it on every layout change and whenever [rows] change; an empty list
     * on screen is reported with empty [rows] and empty [visibleRows]. Repeated calls are cheap.
     */
    fun onLayout(rows: List<Any?>, visibleRows: Collection<Int>) {
        // Before the first layout a non-empty list has no visible rows yet: nothing to report.
        if (visibleRows.isEmpty() && rows.isNotEmpty()) return
        var first = Int.MAX_VALUE
        var last = Int.MIN_VALUE
        for (row in visibleRows) {
            if (row !in rows.indices) continue
            val index = sourceIndex(row, rows[row]) ?: continue
            if (index < first) first = index
            if (index > last) last = index
        }
        // Null: no server rows are visible (an empty list, or only decorations).
        val range = if (first <= last) first..last else null
        if (known && range == lastRange) return
        known = true
        lastRange = range
        // A list shown without rows is shown again once it gets them: it may only have been
        // not read yet, and the paginator decides about the refresh by the first rows.
        val show = !shown || (range != null && !withRows)
        if (show && started) {
            shown = true
            withRows = range != null
            events.onShown(range)
        } else if (range != null) {
            events.onVisibleRangeChanged(range)
        }
    }

    /** The host became visible: a screen opened or returned, the app came to the foreground. */
    fun onStart() {
        started = true
        if (!shown && known) {
            shown = true
            withRows = lastRange != null
            events.onShown(lastRange)
        }
    }

    /**
     * The host is hidden or the list is gone. With [isConfigurationChange] (Android recreating the
     * screen) the list is not reported as hidden.
     */
    fun onStop(isConfigurationChange: Boolean = false) {
        started = false
        if (shown && !isConfigurationChange) {
            shown = false
            events.onHidden()
        }
    }
}

internal fun sourceIndex(uiIndex: Int, item: Any?): Int? =
    (if (item is PagingIndexedItem) item.serverIndex else uiIndex)?.takeIf { it >= 0 }
