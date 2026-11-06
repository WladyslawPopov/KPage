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

/** The show / hide / scroll rules of [PagingListTracker], without any UI. */
class PagingListTrackerTest {

    private val events = RecordingEvents()
    private val tracker = PagingListTracker(events)
    private val rows60 = PagingRowSource.of(List(60) { "row_$it" })
    private val noRows = PagingRowSource.of(emptyList())

    private fun IntRange.rows() = toList()

    @Test
    fun nothing_is_shown_before_start() {
        tracker.onLayout(rows60, (0..9).rows())
        assertTrue(events.shown.isEmpty())

        tracker.onStart()
        assertEquals(listOf<IntRange?>(0..9), events.shown)
    }

    @Test
    fun the_first_layout_is_shown_then_scrolls_are_changes() {
        tracker.onStart()
        tracker.onLayout(rows60, (0..9).rows())
        tracker.onLayout(rows60, (0..9).rows())
        tracker.onLayout(rows60, (5..14).rows())

        assertEquals(listOf<IntRange?>(0..9), events.shown)
        assertEquals(listOf(5..14), events.changed)
    }

    @Test
    fun a_list_not_laid_out_yet_reports_nothing() {
        tracker.onStart()
        tracker.onLayout(rows60, emptyList())

        assertTrue(events.shown.isEmpty())
        assertTrue(events.changed.isEmpty())
    }

    @Test
    fun an_empty_list_is_shown_without_rows_and_again_with_them() {
        tracker.onStart()
        tracker.onLayout(noRows, emptyList())
        tracker.onLayout(rows60, (0..9).rows())

        assertEquals(listOf<IntRange?>(null, 0..9), events.shown)
    }

    @Test
    fun stop_hides_and_start_shows_the_last_range() {
        tracker.onStart()
        tracker.onLayout(rows60, (0..9).rows())
        tracker.onStop()
        tracker.onStart()

        assertEquals(1, events.hidden)
        assertEquals(listOf<IntRange?>(0..9, 0..9), events.shown)
    }

    @Test
    fun decorated_rows_are_read_by_their_server_positions() {
        val rows = pagedListOf<String>(60).decorate<String, String> {
            header("H")
            header("H2")
        }
        tracker.onStart()
        tracker.onLayout(rows, (0..11).rows())

        assertEquals(listOf<IntRange?>(0..9), events.shown)
    }

    @Test
    fun decorations_are_skipped_and_server_indexes_are_used() {
        tracker.onStart()
        tracker.onLayout(indexedRows(), (0..11).rows())

        assertEquals(listOf<IntRange?>(0..9), events.shown)
    }

    @Test
    fun only_decorations_visible_is_a_list_without_rows() {
        val rows = indexedRows()
        tracker.onStart()
        tracker.onLayout(rows, listOf(0, 1))
        tracker.onLayout(rows, (0..5).rows())

        assertEquals(listOf<IntRange?>(null, 0..3), events.shown)
    }

    @Test
    fun visible_rows_in_any_order_and_out_of_bounds() {
        tracker.onStart()
        tracker.onLayout(PagingRowSource.of(List(10) { "row_$it" }), listOf(7, 3, 99, -1, 5))

        assertEquals(listOf<IntRange?>(3..7), events.shown)
    }

    // Two decorations, then the server rows 0..57.
    private fun indexedRows() = PagingRowSource.of(List(60) { IndexedRow(if (it < 2) null else it - 2) })

    private class RecordingEvents : PagingEvents {
        val shown = mutableListOf<IntRange?>()
        val changed = mutableListOf<IntRange>()
        var hidden = 0

        override fun onShown(visible: IntRange?) {
            shown += visible
        }

        override fun onHidden() {
            hidden++
        }

        override fun onVisibleRangeChanged(visible: IntRange) {
            changed += visible
        }
    }

    private data class IndexedRow(override val serverIndex: Int?) : PagingIndexedItem
}
