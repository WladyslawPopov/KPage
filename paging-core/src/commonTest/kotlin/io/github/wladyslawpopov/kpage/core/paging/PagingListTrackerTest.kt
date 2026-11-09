package io.github.wladyslawpopov.kpage.core.paging

import io.github.wladyslawpopov.kpage.core.paging.data.effect.PagingEvents
import io.github.wladyslawpopov.kpage.core.paging.data.effect.PagingIndexedItem
import io.github.wladyslawpopov.kpage.core.paging.data.effect.PagingListTracker
import io.github.wladyslawpopov.kpage.core.paging.data.effect.PagingRowSource
import io.github.wladyslawpopov.kpage.core.paging.data.state.decorate
import io.github.wladyslawpopov.kpage.core.paging.data.state.pagedListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The reporting rules of [PagingListTracker], without any UI. */
class PagingListTrackerTest {

    private val events = RecordingEvents()
    private val tracker = PagingListTracker(events)
    private val rows60 = PagingRowSource.of(List(60) { "row_$it" })
    private val noRows = PagingRowSource.of(emptyList())

    private fun IntRange.rows() = toList()

    @Test
    fun the_visible_range_is_reported_and_only_when_it_changes() {
        tracker.onLayout(rows60, (0..9).rows())
        tracker.onLayout(rows60, (0..9).rows())
        tracker.onLayout(rows60, (5..14).rows())

        assertEquals(listOf(0..9, 5..14), events.changed)
    }

    @Test
    fun a_list_not_laid_out_yet_reports_nothing() {
        tracker.onLayout(rows60, emptyList())

        assertTrue(events.changed.isEmpty())
    }

    @Test
    fun an_empty_list_reports_nothing_and_the_rows_that_arrive_are_reported() {
        tracker.onLayout(noRows, emptyList())
        assertTrue(events.changed.isEmpty())

        tracker.onLayout(rows60, (0..9).rows())
        assertEquals(listOf(0..9), events.changed)
    }

    @Test
    fun decorated_rows_are_read_by_their_server_positions() {
        val rows = pagedListOf<String>(60).decorate<String, String> {
            header("H")
            header("H2")
        }
        tracker.onLayout(rows, (0..11).rows())

        assertEquals(listOf(0..9), events.changed)
    }

    @Test
    fun decorations_are_skipped_and_server_indexes_are_used() {
        tracker.onLayout(indexedRows(), (0..11).rows())

        assertEquals(listOf(0..9), events.changed)
    }

    @Test
    fun only_decorations_visible_reports_nothing_until_a_server_row_is_visible() {
        val rows = indexedRows()
        tracker.onLayout(rows, listOf(0, 1))
        assertTrue(events.changed.isEmpty())

        tracker.onLayout(rows, (0..5).rows())
        assertEquals(listOf(0..3), events.changed)
    }

    @Test
    fun visible_rows_in_any_order_and_out_of_bounds() {
        tracker.onLayout(PagingRowSource.of(List(10) { "row_$it" }), listOf(7, 3, 99, -1, 5))

        assertEquals(listOf(3..7), events.changed)
    }

    // Two decorations, then the server rows 0..57.
    private fun indexedRows() = PagingRowSource.of(List(60) { IndexedRow(if (it < 2) null else it - 2) })

    private class RecordingEvents : PagingEvents {
        val changed = mutableListOf<IntRange>()

        override fun onVisibleRangeChanged(visible: IntRange) {
            changed += visible
        }
    }

    private data class IndexedRow(override val serverIndex: Int?) : PagingIndexedItem
}
