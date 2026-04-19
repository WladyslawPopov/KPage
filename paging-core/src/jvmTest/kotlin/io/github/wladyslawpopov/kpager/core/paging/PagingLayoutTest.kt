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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import io.github.wladyslawpopov.kpager.core.paging.data.FakeItem
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class PagingLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun test_paging_layout_renders_and_scrolls() {
        // Arrange: статический список из 60 заглушек (1 страница)
        val items = List(60) { FakeItem("id_$it", "Item $it") }
        var prefetchedPage = -1

        composeTestRule.setContent {
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

        // Assert Render: Проверяем, что первый элемент виден
        composeTestRule.onNodeWithTag("item_id_0").assertIsDisplayed()

        // Act: Скроллим до порогового значения (например, 50)
        composeTestRule.onNode(hasScrollToIndexAction()).performScrollToIndex(50)
        composeTestRule.waitForIdle()

        // Assert Prefetch: Логика PagingLayout:
        // currentPage = 50 / 60 = 0
        // itemsInCurrentPage = 50 % 60 = 50.
        // Threshold = 60 - (60 / 4) = 45. 50 >= 45, значит триггерится страница 0!
        assertEquals(0, prefetchedPage, "onPrefetch должен сработать для текущей видимой страницы")
    }

    @Test
    fun test_paging_layout_retains_scroll_on_data_update() {
        // Arrange: Отрисовать 60 элементов
        var items by mutableStateOf(List(60) { FakeItem("id_$it", "Item $it") })

        composeTestRule.setContent {
            val state = rememberLazyGridState()
            PagingLayout(
                items = items,
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

        // Act: Пользователь доскроллил до 30-го элемента
        composeTestRule.onNode(hasScrollToIndexAction()).performScrollToIndex(30)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("item_id_30").assertIsDisplayed()

        // Act: Имитируем обновление items (добавление еще 60 элементов, всего 120)
        items = List(120) { FakeItem("id_$it", "Item $it") }
        composeTestRule.waitForIdle()

        // Assert: Видимый элемент на экране не должен "прыгнуть".
        // Индекс 30 по-прежнему должен быть отрисован и остаться в иерархии!
        composeTestRule.onNodeWithTag("item_id_30").assertIsDisplayed()
        
        // Можем даже убедиться, что новые данные тоже можно доскроллить
        composeTestRule.onNode(hasScrollToIndexAction()).performScrollToIndex(110)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("item_id_110").assertIsDisplayed()
    }
}
