package io.github.wladyslawpopov.kpager.core.paging

import app.cash.sqldelight.db.SqlDriver
import io.github.wladyslawpopov.kpager.cache.PagingDataBase
import io.github.wladyslawpopov.kpager.core.paging.data.FakeApi
import io.github.wladyslawpopov.kpager.core.paging.data.FakeItem
import io.github.wladyslawpopov.kpager.core.paging.data.LoadState
import io.github.wladyslawpopov.kpager.core.paging.data.TestPaginatorFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StablePaginatorTest {

    private lateinit var fakeApi: FakeApi
    private lateinit var database: PagingDataBase
    private lateinit var paginator: StablePaginator<FakeItem>
    private lateinit var driver: SqlDriver

    @Before
    fun setup() {
        fakeApi = FakeApi()
        driver = TestPaginatorFactory.createInMemoryDriver()
        database = PagingDataBase(driver)
    }

    @After
    fun tearDown() {
        if (::paginator.isInitialized) {
            paginator.close()
        }
    }

    @Test
    fun test_initial_load() = runTest {
        // Arrange
        paginator = TestPaginatorFactory.createPaginator(fakeApi, driver)

        // Act
        assertEquals(LoadState.IDLE, paginator.loadState.value)
        paginator.reset(0)

        // Wait until both pages are loaded (prefetchDistance = 1 behavior)
        val items = paginator.itemsMap.filter { it.size == fakeApi.pageSize * 2 }.first()

        // Assert
        val currentState = paginator.loadState.value
        assertTrue(
            currentState is LoadState.IDLE || currentState is LoadState.END,
            "State should transition to IDLE or END after a successful load"
        )

        // Check DB
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
        // Arrange: pre-populate database with "old" data (1 page)
        TestPaginatorFactory.prePopulateDatabase(database, pages = 1, pageSize = fakeApi.pageSize)
        fakeApi.networkDelayMs = 200L // Artificial API delay

        paginator = TestPaginatorFactory.createPaginator(fakeApi, driver)

        // Act & Assert 1: Retrieve old cache
        // Strictly wait until cache size equals page size
        val cachedData = paginator.itemsMap.filter { it.size >= fakeApi.pageSize }.first()

        assertEquals(fakeApi.pageSize, cachedData.size, "Cache should be emitted before network response")
        assertEquals(
            cachedData[0]?.title?.startsWith("Old"),
            true,
            "Cache should contain old data (DB stubs)"
        )

        // Act 2: Refresh
        paginator.reset(0)

        // Assert 2: Wait for new data to arrive (size 40 and new title)
        val newData = paginator.itemsMap.filter { map ->
            map.size >= fakeApi.pageSize * 2 && map[0]?.title?.startsWith("Item") == true
        }.first()

        assertEquals(
            fakeApi.pageSize * 2,
            newData.size,
            "New pages must be loaded (target and target+1)"
        )
    }

    @Test
    fun test_endless_scroll_forward_and_db_pruning() = runTest {
        // Arrange
        fakeApi.totalPages = 10 // Increase total pages to allow deep scrolling
        paginator = TestPaginatorFactory.createPaginator(fakeApi, driver)
        paginator.reset(0)

        // Wait for initial load (pages 0 and 1)
        paginator.itemsMap.filter { it.size == fakeApi.pageSize * 2 }.first()

        // Act: Scroll up to page 6
        for (page in 2..6) {
            paginator.onPrefetch(page)
            val expectedGlobalIndex = page * fakeApi.pageSize
            // Wait for the requested page to appear in the map
            paginator.itemsMap.filter { it.containsKey(expectedGlobalIndex) }.first()
        }

        // Wait for page 7 to also appear because prefetchDistance = 1 loads target + 1
        paginator.itemsMap.filter { it.containsKey(7 * fakeApi.pageSize) }.first()

        // Assert DB Pruning:
        val itemsMap = paginator.itemsMap.value
        val dbPages = database.listingEntryQueries.getExistingPageNumbers("test_query").executeAsList()

        // Because maxPagesInDb(3) + buffer(2) = 5, and prefetch loads target + 1,
        // scrolling to 6 actually loads up to page 7.
        // The paginator will prune anything beyond the 5 most recent pages.
        // Thus, pages 0, 1, and 2 are pruned, leaving exactly [3, 4, 5, 6, 7].
        assertEquals(
            listOf<Long>(3L, 4L, 5L, 6L, 7L),
            dbPages.sorted(),
            "Old pages (0, 1, 2) must be cleared from the DB, keeping the allowed 5 pages"
        )

        assertEquals(
            fakeApi.pageSize * 5,
            itemsMap.size,
            "UI Map contains exactly the 5 allowed pages"
        )
    }

    @Test
    fun test_reverse_scrolling_recovery() = runTest {
        // Arrange: First scroll down so old pages are deleted (calls the previous test)
        test_endless_scroll_forward_and_db_pruning()

        // Act: Suddenly return to the top of the scroll (requesting page 0)
        paginator.onPrefetch(0)

        // Wait for the 0th element to reappear in the map
        val itemsMap = paginator.itemsMap.filter { it.containsKey(0) }.first()

        // Assert
        val dbPages = database.listingEntryQueries.getExistingPageNumbers("test_query").executeAsList()
        assertTrue(dbPages.contains(0L), "Page zero must be reloaded into the database")
        assertEquals("id_0", itemsMap[0]?.id)
    }

    @Test
    fun test_update_single_item_optimistic() = runTest {
        // Arrange
        paginator = TestPaginatorFactory.createPaginator(fakeApi, driver)
        paginator.reset(0)

        // Wait for initial data
        val items = paginator.itemsMap.filter { it.containsKey(0) }.first()
        val itemToUpdate = items[0]!!
        val updatedItem = itemToUpdate.copy(title = "Liked Item")

        // Act: Update a single item
        paginator.updateItem(updatedItem.id, updatedItem)

        // Assert: Wait for the updated title to arrive in the Flow
        val newMap = paginator.itemsMap.filter { map ->
            map[0]?.title == "Liked Item"
        }.first()

        assertEquals("Liked Item", newMap[0]?.title, "UI Flow should receive the update")

        // Check DB
        val dbItemRaw = database.itemRawQueries.selectById(updatedItem.id).executeAsOne()
        assertTrue(dbItemRaw.json.contains("Liked Item"), "DB must save the new data")
    }

    @Test
    fun test_end_of_list() = runTest {
        // Arrange: Return only one page (isMore = false)
        fakeApi.totalPages = 1
        paginator = TestPaginatorFactory.createPaginator(fakeApi, driver)

        // Act
        paginator.reset(0)

        // Assert: Wait until state becomes END
        val endState = paginator.loadState.filter { it is LoadState.END }.first()

        assertEquals(LoadState.END, endState, "Paginator should switch to END")
    }

    @Test
    fun test_network_error_with_cache() = runTest {
        // Arrange: Load the first pages successfully
        paginator = TestPaginatorFactory.createPaginator(fakeApi, driver)
        paginator.reset(0)

        // Wait for initial data to be fully loaded (pages 0 and 1)
        paginator.itemsMap.filter { it.size == fakeApi.pageSize * 2 }.first()

        // Configure API to return an error for the next page
        fakeApi.shouldFail = true

        // Act: Try to fetch page 2 (since 0 and 1 are already loaded)
        paginator.onPrefetch(2)

        // Assert: Expect ERROR state
        val errorState = paginator.loadState.filter { it is LoadState.ERROR }.first()
        assertTrue((errorState as LoadState.ERROR).error.contains("ServerErrorException"))

        // Cache should remain intact
        val currentItems = paginator.itemsMap.value
        assertTrue(
            currentItems.size >= fakeApi.pageSize * 2,
            "Data must not disappear on network error"
        )
    }

    @Test
    fun test_deep_link_initialization() = runTest {
        // Arrange: API supports many pages
        fakeApi.totalPages = 20
        paginator = TestPaginatorFactory.createPaginator(fakeApi, driver)

        // Act: User opens the app directly at index 100 (e.g., from a deep link or restored state)
        // In FakeApi, pageSize is 20. So Index 100 / 20 (pageSize) = Page 5.
        paginator.reset(index = 100)

        // Assert: Wait until the map contains the specific deep index
        val items = paginator.itemsMap.filter { it.containsKey(100) }.first()

        // Check that the paginator correctly calculated the target page
        val dbPages = database.listingEntryQueries.getExistingPageNumbers("test_query").executeAsList()

        assertTrue(dbPages.contains(5L), "Paginator must load the target page (Page 5)")
        assertEquals("id_100", items[100]?.id, "The exact item at index 100 must be available")
    }

    @Test
    fun test_concurrent_prefetch_spam_protection() = runTest {
        // Arrange
        paginator = TestPaginatorFactory.createPaginator(fakeApi, driver)
        paginator.reset(0)
        paginator.itemsMap.filter { it.size == fakeApi.pageSize * 2 }.first()

        // Act: UI bugs out and spams prefetch for the exact same page 10 times concurrently
        for (i in 1..10) {
            launch { paginator.onPrefetch(2) }
        }

        // Wait for page 2 to load
        val items = paginator.itemsMap.filter { it.containsKey(2 * fakeApi.pageSize) }.first()

        // Assert
        // If mutex didn't work, we'd have duplicate entries or crashes.
        // Size should be exactly 3 pages (0, 1, 2) = 180 items.
        assertEquals(
            fakeApi.pageSize * 3,
            items.size,
            "Paginator must ignore duplicate concurrent requests for the same page"
        )
    }
}
