package io.github.wladyslawpopov.kpage.core.paging

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.wladyslawpopov.kpage.core.paging.data.FakeItem
import io.github.wladyslawpopov.kpage.core.paging.data.effect.PagingEffect
import io.github.wladyslawpopov.kpage.core.paging.data.effect.PagingEvents
import io.github.wladyslawpopov.kpage.core.paging.data.effect.PagingIndexedItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** What `PagingEffect` reports to the paginator: visible server positions, shown and hidden. */
@OptIn(ExperimentalTestApi::class)
class PagingEffectTest {

    private val loaded = List(60) { FakeItem("id_$it", "Item $it") }

    @Composable
    private fun TestList(items: List<Any?>, events: PagingEvents) {
        val state = rememberLazyListState()
        PagingEffect(state, items, events)
        LazyColumn(state = state) {
            items(items.size) { Box(Modifier.size(100.dp)) }
        }
    }

    @Test
    fun back_and_forth_jumps_report_only_the_current_position() = runComposeUiTest {
        val events = RecordingEvents()
        setContent { TestList(List(600) { FakeItem("id_$it", "Loaded") }, events) }

        repeat(5) {
            onNode(hasScrollToIndexAction()).performScrollToIndex(180)
            waitUntil(timeoutMillis = 5_000L) { events.firsts.lastOrNull() == 180 }
            onNode(hasScrollToIndexAction()).performScrollToIndex(360)
            waitUntil(timeoutMillis = 5_000L) { events.firsts.lastOrNull() == 360 }
        }
        runOnIdle { assertEquals(listOf(0) + List(5) { listOf(180, 360) }.flatten(), events.firsts) }
    }

    @Test
    fun rows_with_a_server_index_skip_the_header_rows() = runComposeUiTest {
        val events = RecordingEvents()
        setContent {
            TestList(List(122) { IndexedRow(if (it < 2) null else it - 2) }, events)
        }
        onNode(hasScrollToIndexAction()).performScrollToIndex(61)
        waitUntil(timeoutMillis = 5_000L) { events.firsts.lastOrNull() == 59 }
        runOnIdle { assertEquals(listOf(0, 59), events.firsts, "UI row 61 is server position 59") }
    }

    @Test
    fun the_whole_visible_range_is_reported() = runComposeUiTest {
        val events = RecordingEvents()
        setContent { TestList(loaded, events) }

        waitUntil(timeoutMillis = 5_000L) { events.shown.isNotEmpty() }
        runOnIdle {
            val range = events.shown.single()!!
            assertEquals(0, range.first)
            assertTrue(range.last > 0, "Every visible row counts, not only the first: $range")
            assertTrue(events.changed.isEmpty(), "The first rows go through onShown")
        }
    }

    @Test
    fun an_empty_list_is_shown_without_rows() = runComposeUiTest {
        val events = RecordingEvents()
        setContent { TestList(emptyList<FakeItem>(), events) }

        waitUntil(timeoutMillis = 5_000L) { events.shown.isNotEmpty() }
        runOnIdle { assertEquals(listOf<IntRange?>(null), events.shown) }
    }

    @Test
    fun a_list_shown_without_rows_is_shown_again_once_it_gets_them() = runComposeUiTest {
        val events = RecordingEvents()
        var items by mutableStateOf<List<FakeItem>>(emptyList())
        setContent { TestList(items, events) }
        waitUntil(timeoutMillis = 5_000L) { events.shown.isNotEmpty() }

        items = loaded
        waitUntil(timeoutMillis = 5_000L) { events.shown.size == 2 }
        runOnIdle { assertEquals(0, events.shown[1]?.first) }
    }

    @Test
    fun leaving_the_composition_reports_hidden() = runComposeUiTest {
        val events = RecordingEvents()
        var visible by mutableStateOf(true)
        setContent { if (visible) TestList(loaded, events) }
        waitUntil(timeoutMillis = 5_000L) { events.shown.isNotEmpty() }

        visible = false
        waitUntil(timeoutMillis = 5_000L) { events.hidden == 1 }

        visible = true
        waitUntil(timeoutMillis = 5_000L) { events.shown.size == 2 }
        runOnIdle { assertEquals(listOf(0, 0), events.shown.map { it?.first }, "Coming back is a new onShown") }
    }

    @Test
    fun a_new_events_object_gets_its_own_onShown() = runComposeUiTest {
        val first = RecordingEvents()
        val second = RecordingEvents()
        var events by mutableStateOf<PagingEvents>(first)
        setContent { TestList(loaded, events) }
        waitUntil(timeoutMillis = 5_000L) { first.shown.isNotEmpty() }

        events = second
        waitUntil(timeoutMillis = 5_000L) { second.shown.isNotEmpty() }
        runOnIdle {
            assertEquals(0, second.shown.single()?.first)
            assertEquals(1, first.hidden, "The old paginator is told the list is gone")
        }
    }

    /** [firsts] collects the first visible position of every report (shown or changed). */
    private class RecordingEvents : PagingEvents {
        val firsts = mutableListOf<Int>()
        val shown = mutableListOf<IntRange?>()
        val changed = mutableListOf<IntRange>()
        var hidden = 0

        override fun onShown(visible: IntRange?) {
            shown += visible
            visible?.let { firsts += it.first }
        }

        override fun onHidden() {
            hidden++
        }

        override fun onVisibleRangeChanged(visible: IntRange) {
            changed += visible
            firsts += visible.first
        }
    }

    private data class IndexedRow(override val serverIndex: Int?) : PagingIndexedItem
}
