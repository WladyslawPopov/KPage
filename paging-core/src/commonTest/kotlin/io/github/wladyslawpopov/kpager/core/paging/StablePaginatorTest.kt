package io.github.wladyslawpopov.kpager.core.paging

import io.github.wladyslawpopov.kpager.cache.PagingDataBase
import io.github.wladyslawpopov.kpager.core.paging.data.FakeApi
import io.github.wladyslawpopov.kpager.core.paging.data.FakeItem
import io.github.wladyslawpopov.kpager.core.paging.data.LoadState
import io.github.wladyslawpopov.kpager.core.paging.data.TestPaginatorFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StablePaginatorTest {

    private lateinit var fakeApi: FakeApi
    private lateinit var database: PagingDataBase
    private lateinit var paginator: StablePaginator<FakeItem>

    @BeforeTest
    fun setup() {
        fakeApi = FakeApi()
        fakeApi.networkDelayMs = 0
        database = TestPaginatorFactory.createInMemoryDriver()
    }

    @AfterTest
    fun tearDown() {
        if (::paginator.isInitialized) {
            paginator.close()
        }
    }

    @Test
    fun test_initial_load() = runTest {
        paginator = TestPaginatorFactory.createPaginator(fakeApi, database)
        assertEquals(LoadState.IDLE, paginator.loadState.value)

        paginator.reset(0)
        val items = paginator.itemsMap.first { it.size >= fakeApi.pageSize * 2 }

        val currentState = paginator.loadState.value
        assertTrue(
            currentState is LoadState.IDLE || currentState is LoadState.END,
            "State should transition to IDLE or END after a successful load"
        )

        val rawItems = database.itemRawQueries.selectById("id_0").executeAsOneOrNull()
        assertTrue(rawItems != null, "Items must be stored in ItemRaw")

        val listingEntries = database.listingEntryQueries.getExistingPageNumbers("test_query").executeAsList()
        assertTrue(listingEntries.contains(0L), "Page zero must be in the cache")

        assertEquals(
            fakeApi.pageSize * 2,
            items.size,
            "Both requested pages must be loaded into the Map"
        )
    }

    @Test
    fun test_cache_first_display_and_network_refresh() = runTest {
        TestPaginatorFactory.prePopulateDatabase(database, pages = 1, pageSize = fakeApi.pageSize)
        fakeApi.networkDelayMs = 10L

        paginator = TestPaginatorFactory.createPaginator(fakeApi, database)

        val cachedData = paginator.itemsMap.first { it.size >= fakeApi.pageSize }

        assertEquals(fakeApi.pageSize, cachedData.size, "Cache should be emitted before network response")
        assertEquals(
            cachedData[0]?.title?.startsWith("Old"),
            true,
            "Cache should contain old data (DB stubs)"
        )

        fakeApi.networkDelayMs = 0L
        paginator.reset(0)

        val newData = paginator.itemsMap.first { map ->
            map.size >= fakeApi.pageSize * 2 && map[0]?.title?.startsWith("Item") == true
        }

        assertEquals(
            fakeApi.pageSize * 2,
            newData.size,
            "New pages must be loaded (target and target+1)"
        )
    }

    private suspend fun performEndlessScrollForward() {
        fakeApi.totalPages = 10
        paginator = TestPaginatorFactory.createPaginator(fakeApi, database)
        paginator.reset(0)

        paginator.itemsMap.first { it.size >= fakeApi.pageSize * 2 }

        for (page in 2..6) {
            paginator.onPrefetch(page)
            val expectedGlobalIndex = page * fakeApi.pageSize
            paginator.itemsMap.first { it.containsKey(expectedGlobalIndex) }
        }

        paginator.itemsMap.first { it.containsKey(7 * fakeApi.pageSize) }
    }

    @Test
    fun test_endless_scroll_forward_and_db_pruning() = runTest {
        performEndlessScrollForward()

        val itemsMap = paginator.itemsMap.value
        val dbPages = database.listingEntryQueries.getExistingPageNumbers("test_query").executeAsList()

        assertEquals(
            listOf(3L, 4L, 5L, 6L, 7L),
            dbPages.sorted(),
            "Old pages (0, 1, 2) must be cleared from the DB"
        )

        assertEquals(
            fakeApi.pageSize * 5,
            itemsMap.size,
            "UI Map contains exactly the 5 allowed pages"
        )
    }

    @Test
    fun test_reverse_scrolling_recovery() = runTest {
        performEndlessScrollForward()

        paginator.onPrefetch(0)

        val itemsMap = paginator.itemsMap.first { it.containsKey(0) }

        val dbPages = database.listingEntryQueries.getExistingPageNumbers("test_query").executeAsList()
        assertTrue(dbPages.contains(0L), "Page zero must be reloaded into the database")
        assertEquals("id_0", itemsMap[0]?.id)
    }

    @Test
    fun test_update_single_item_optimistic() = runTest {
        paginator = TestPaginatorFactory.createPaginator(fakeApi, database)
        paginator.reset(0)

        val items = paginator.itemsMap.first { it.containsKey(0) }
        val itemToUpdate = items[0]!!
        val updatedItem = itemToUpdate.copy(title = "Liked Item")

        paginator.updateItem(updatedItem.id, updatedItem)

        val newMap = paginator.itemsMap.first { map ->
            map[0]?.title == "Liked Item"
        }

        assertEquals("Liked Item", newMap[0]?.title, "UI Flow should receive the update")

        val dbItemRaw = database.itemRawQueries.selectById(updatedItem.id).executeAsOne()
        assertTrue(dbItemRaw.json.contains("Liked Item"), "DB must save the new data")
    }

    @Test
    fun test_end_of_list() = runTest {
        fakeApi.totalPages = 1
        paginator = TestPaginatorFactory.createPaginator(fakeApi, database)
        paginator.reset(0)

        val endState = paginator.loadState.filterIsInstance<LoadState.END>().first()
        assertEquals(LoadState.END, endState)
    }

    @Test
    fun test_network_error_with_cache() = runTest {
        paginator = TestPaginatorFactory.createPaginator(fakeApi, database)
        paginator.reset(0)
        paginator.itemsMap.first { it.size >= fakeApi.pageSize * 2 }

        fakeApi.shouldFail = true
        paginator.onPrefetch(2)

        val errorState = paginator.loadState.filterIsInstance<LoadState.ERROR>().first()
        assertTrue(errorState.error.contains("ServerErrorException"))

        val currentItems = paginator.itemsMap.value
        assertTrue(currentItems.size >= fakeApi.pageSize * 2)
    }

    @Test
    fun test_deep_link_initialization() = runTest {
        fakeApi.totalPages = 20
        paginator = TestPaginatorFactory.createPaginator(fakeApi, database)
        paginator.reset(index = 100)

        val items = paginator.itemsMap.first { it.containsKey(100) }
        val dbPages = database.listingEntryQueries.getExistingPageNumbers("test_query").executeAsList()

        assertTrue(dbPages.contains(5L))
        assertEquals("id_100", items[100]?.id)
    }

    @Test
    fun test_concurrent_prefetch_spam_protection() = runTest {
        paginator = TestPaginatorFactory.createPaginator(fakeApi, database)
        paginator.reset(0)
        paginator.itemsMap.first { it.size >= fakeApi.pageSize * 2 }

        repeat(10) {
            launch { paginator.onPrefetch(2) }
        }

        val items = paginator.itemsMap.first { it.containsKey(2 * fakeApi.pageSize) }

        assertEquals(
            fakeApi.pageSize * 3,
            items.size,
            "Paginator must ignore duplicate concurrent requests"
        )
    }
}
