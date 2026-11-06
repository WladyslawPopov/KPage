package io.github.wladyslawpopov.kpage.core.paging.data.effect

/**
 * The rows of a list UI as [PagingListTracker] reads them: how many there are and the server
 * position of each. Read by index, so a UI outside Kotlin (SwiftUI, UIKit) passes the rows by
 * reference instead of converting a list as long as the total count. `PagingRows` (the result of
 * `decorate`) is one; [of] adapts any list.
 */
interface PagingRowSource {
    val rowCount: Int

    /** The server position of [row]; null for a decoration or a row out of range. */
    fun serverIndexAt(row: Int): Int?

    companion object {
        /** Rows implementing [PagingIndexedItem] give their server index; other rows use their position. */
        fun of(rows: List<Any?>): PagingRowSource = ListRowSource(rows)
    }
}

private class ListRowSource(private val rows: List<Any?>) : PagingRowSource {
    override val rowCount: Int get() = rows.size

    override fun serverIndexAt(row: Int): Int? = if (row in rows.indices) sourceIndex(row, rows[row]) else null
}
