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
import io.github.wladyslawpopov.kpager.core.paging.data.FakeItem
import kotlin.test.Test
import kotlin.test.assertEquals

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
                onPrefetch = { page -> prefetchedPage = page },
                content = { _, item ->
                    Box(Modifier.size(100.dp).testTag("item_${item?.id}")) {
                        Text(item?.title ?: "Placeholder")
                    }
                }
            )
        }

        onNodeWithTag("item_id_0").assertIsDisplayed()

        // Скроллим до 50, чтобы индекс 50 стал видимым. 
        // По логике PagingLayout (threshold = 60 - 15 = 45), это должно триггернуть onPrefetch(0).
        onNode(hasScrollToIndexAction()).performScrollToIndex(50)
        waitForIdle()

        // Даем немного времени LaunchedEffect сработать, если waitForIdle не дождался коллекта
        waitUntil(timeoutMillis = 5000L) { prefetchedPage == 0 }

        assertEquals(0, prefetchedPage, "onPrefetch должен сработать для текущей видимой страницы")
    }

    @Test
    fun test_paging_layout_retains_scroll_on_data_update() = runComposeUiTest {
        var itemsList by mutableStateOf(List(60) { FakeItem("id_$it", "Item $it") })

        setContent {
            val state = rememberLazyGridState()
            PagingLayout(
                items = itemsList,
                totalCount = 1000,
                pageSize = 60,
                state = state,
                keyExtractor = { _, item -> item.id },
                onPrefetch = { },
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
    }
}
