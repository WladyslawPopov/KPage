package io.github.wladyslawpopov.kpager.core.paging

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.wladyslawpopov.kpager.core.paging.data.PagingIndexedItem
import io.github.wladyslawpopov.kpager.core.paging.data.FakeItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PagingLayoutTest {

    @Test
    fun test_paging_layout_renders_and_scrolls() = runComposeUiTest {
        val items = List(60) { FakeItem("id_$it", "Item $it") }
        var prefetchedPage = -1

        setContent {
            PagingLayout(
                items = items,
                totalCount = 1000,
                pageSize = 60,
                keyExtractor = { _, item -> item.id },
                onVisiblePageChanged = { page -> prefetchedPage = page },
                content = { _, item ->
                    Box(Modifier.size(100.dp).testTag("item_${item?.id}")) {
                        Text(item?.title ?: "Placeholder")
                    }
                }
            )
        }

        onNodeWithTag("item_id_0").assertIsDisplayed()

        // Scrolling within page zero only reports the visible page, never a page to fetch.
        onNode(hasScrollToIndexAction()).performScrollToIndex(50)
        waitForIdle()

        // Даем немного времени LaunchedEffect сработать, если waitForIdle не дождался коллекта
        waitUntil(timeoutMillis = 5000L) { prefetchedPage == 0 }

        assertEquals(0, prefetchedPage, "onVisiblePageChanged должен сработать для текущей видимой страницы")
    }

    @Test
    fun test_paging_layout_retains_scroll_on_data_update() = runComposeUiTest {
        var itemsList by mutableStateOf(List(60) { FakeItem("id_$it", "Item $it") })
        val positions = mutableListOf<Int>()

        setContent {
            val state = rememberLazyGridState()
            PagingLayout(
                items = itemsList,
                totalCount = 1000,
                pageSize = 60,
                state = state,
                keyExtractor = { _, item -> item.id },
                onVisiblePageChanged = { positions.add(it) },
                content = { _, item ->
                    Box(Modifier.size(100.dp).testTag("item_${item?.id}")) {
                        Text(item?.title ?: "Placeholder")
                    }
                }
            )
        }

        onNode(hasScrollToIndexAction()).performScrollToIndex(30)
        waitForIdle()
        onNodeWithTag("item_id_30").assertIsDisplayed()

        // Обновляем список, добавляя новые элементы. Keys стабильны.
        itemsList = List(120) { FakeItem("id_$it", "Item $it") }
        waitForIdle()

        // Scroll position should be preserved thanks to stable keys
        onNodeWithTag("item_id_30").assertIsDisplayed()
        
        onNode(hasScrollToIndexAction()).performScrollToIndex(110)
        waitForIdle()
        onNodeWithTag("item_id_110").assertIsDisplayed()
        waitUntil(timeoutMillis = 5_000L) { positions.lastOrNull() == 1 }
        runOnIdle { assertEquals(listOf(0, 1), positions) }
    }

    @Test
    fun test_back_and_forth_jumps_report_only_current_page() = runComposeUiTest {
        val requests = mutableListOf<Int>()
        setContent {
            PagingLayout(
                items = List(600) { FakeItem("id_$it", "Loaded") }, totalCount = 600,
                keyExtractor = { _, item -> item.id }, onVisiblePageChanged = { requests.add(it) },
                content = { _, _ -> Box(Modifier.size(100.dp)) }
            )
        }
        repeat(5) {
            onNode(hasScrollToIndexAction()).performScrollToIndex(180)
            waitUntil(timeoutMillis = 5_000L) { requests.lastOrNull() == 3 }
            onNode(hasScrollToIndexAction()).performScrollToIndex(360)
            waitUntil(timeoutMillis = 5_000L) { requests.lastOrNull() == 6 }
        }
        runOnIdle { assertEquals(listOf(0) + List(5) { listOf(3, 6) }.flatten(), requests) }
    }

    @Test
    fun test_header_offset_uses_server_index_for_missing_item() = runComposeUiTest {
        val requests = mutableListOf<Int>()
        setContent {
            PagingLayout(
                items = List(122) {
                    IndexedRow("ui_$it", if (it < 2) null else it - 2, if (it == 61) null else "Loaded")
                },
                totalCount = 120, keyExtractor = { _, item -> item.id },
                onVisiblePageChanged = { requests.add(it) },
                content = { _, _ -> Box(Modifier.size(100.dp)) }
            )
        }
        onNode(hasScrollToIndexAction()).performScrollToIndex(61)
        waitUntil(timeoutMillis = 5_000L) { 0 in requests }
        runOnIdle { assertEquals(0, requests.last()) }
    }


    private data class IndexedRow(
        val id: String,
        override val serverIndex: Int?,
        val data: String?,
    ) : PagingIndexedItem

}
