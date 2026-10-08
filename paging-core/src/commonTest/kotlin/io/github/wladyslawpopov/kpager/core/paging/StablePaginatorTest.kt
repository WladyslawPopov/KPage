package io.github.wladyslawpopov.kpager.core.paging

import io.github.wladyslawpopov.kpager.cache.PagingDataBase
import io.github.wladyslawpopov.kpager.core.paging.data.FakeApi
import io.github.wladyslawpopov.kpager.core.paging.data.FakeItem
import io.github.wladyslawpopov.kpager.core.paging.data.LoadState
import io.github.wladyslawpopov.kpager.core.paging.data.TestPaginatorFactory
import io.github.wladyslawpopov.kpager.core.paging.data.PagerPayload
import io.github.wladyslawpopov.kpager.core.paging.data.PaginatorConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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
import kotlin.test.assertSame

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

        val currentState = paginator.loadState.first { it is LoadState.IDLE || it is LoadState.END }
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
            paginator.onVisiblePageChanged(page)
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

        paginator.onVisiblePageChanged(0)

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
        paginator.onVisiblePageChanged(2)

        val errorState = paginator.loadState.filterIsInstance<LoadState.ERROR>().first()
        assertTrue(errorState.error.message?.contains("ServerErrorException") == true)

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
            launch { paginator.onVisiblePageChanged(2) }
        }

        val items = paginator.itemsMap.first { it.size >= fakeApi.pageSize * 4 }

        assertEquals(
            fakeApi.pageSize * 4,
            items.size,
            "Paginator must ignore duplicate concurrent requests"
        )
    }

    private fun controlledPaginator(fetch: suspend (Int) -> PagerPayload<FakeItem>) = StablePaginator(
        serializer = FakeItem.serializer(),
        queryKey = "test_query",
        config = PaginatorConfig(pageSize = 1, prefetchDistance = 0),
        getPage = fetch,
        idExtractor = { it.id },
        db = database
    )

    @Test
    fun test_close_cancels_in_flight_request() = runTest {
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        paginator = controlledPaginator {
            started.complete(Unit)
            try {
                awaitCancellation()
            } finally {
                cancelled.complete(Unit)
            }
        }
        paginator.reset(0)
        started.await()
        paginator.close()
        withContext(Dispatchers.Default) { withTimeout(2_000) { cancelled.await() } }
    }

    @Test
    fun test_reset_cancels_previous_request() = runTest {
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        var calls = 0
        paginator = controlledPaginator {
            if (++calls == 1) {
                started.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    cancelled.complete(Unit)
                }
            } else {
                PagerPayload(1, false, listOf(FakeItem("id_0", "Fresh")))
            }
        }
        paginator.reset(0)
        started.await()
        paginator.reset(0)
        withContext(Dispatchers.Default) { withTimeout(2_000) { cancelled.await() } }
        paginator.itemsMap.first { it[0]?.title == "Fresh" }
    }

    @Test
    fun test_reset_discards_late_response_even_if_fetch_ignores_cancellation() = runTest {
        val started = CompletableDeferred<Job>()
        val releaseOldRequest = CompletableDeferred<Unit>()
        var calls = 0
        paginator = controlledPaginator {
            if (++calls == 1) {
                started.complete(currentCoroutineContext()[Job]!!)
                withContext(NonCancellable) { releaseOldRequest.await() }
                PagerPayload(99, true, listOf(FakeItem("id_0", "Stale")))
            } else {
                PagerPayload(1, false, listOf(FakeItem("id_0", "Fresh")))
            }
        }
        try {
            paginator.reset(0)
            val oldJob = started.await()
            paginator.reset(0)
            paginator.itemsMap.first { it[0]?.title == "Fresh" }
            paginator.loadState.first { it == LoadState.END }
            releaseOldRequest.complete(Unit)
            oldJob.join()
            assertEquals(1, paginator.totalCount.value)
            assertEquals("Fresh", paginator.getItemById("id_0")?.title)
            assertEquals(LoadState.END, paginator.loadState.value)
        } finally {
            releaseOldRequest.complete(Unit)
        }
    }


    @Test
    fun test_cancelled_request_can_be_retried_without_error() = runTest {
        val started = CompletableDeferred<Job>()
        var calls = 0
        paginator = controlledPaginator {
            if (++calls == 1) {
                started.complete(currentCoroutineContext()[Job]!!)
                throw CancellationException("Request cancelled")
            }
            PagerPayload(1, false, listOf(FakeItem("id_0", "Retry")))
        }
        paginator.reset(0)
        started.await().join()
        assertEquals(LoadState.IDLE, paginator.loadState.value)
        paginator.onVisiblePageChanged(0)
        withContext(Dispatchers.Default) {
            withTimeout(2_000) { paginator.itemsMap.first { it[0]?.title == "Retry" } }
        }
    }

    @Test
    fun test_rapid_resets_cancel_every_superseded_request() = runTest {
        val requests = kotlinx.coroutines.channels.Channel<Job>(kotlinx.coroutines.channels.Channel.UNLIMITED)
        val finish = CompletableDeferred<Unit>()
        paginator = controlledPaginator {
            requests.send(currentCoroutineContext()[Job]!!)
            finish.await()
            PagerPayload(1, false, listOf(FakeItem("id_0", "Latest")))
        }
        val superseded = mutableListOf<Job>()
        try {
            repeat(50) {
                paginator.reset(0)
                superseded.add(requests.receive())
            }
            paginator.reset(0)
            val latest = requests.receive()
            withContext(Dispatchers.Default) {
                withTimeout(2_000) { superseded.forEach { it.join() } }
            }
            assertTrue(superseded.all { it.isCancelled })
            assertTrue(latest.isActive)
            assertEquals(LoadState.INITIAL, paginator.loadState.value)
            finish.complete(Unit)
            latest.join()
            assertEquals("Latest", paginator.getItemById("id_0")?.title)
            assertEquals(LoadState.END, paginator.loadState.value)
        } finally {
            finish.complete(Unit)
            requests.close()
        }
    }

    @Test
    fun test_late_prefetch_preserves_visible_page() = runTest {
        val db = TestPaginatorFactory.createInMemoryDriver()
        for (page in 6..10) {
            db.itemRawQueries.insertItemIfAbsent("id_$page", """{"id":"id_$page","title":"Cached"}""", 1)
            db.listingEntryQueries.insertEntryIfAbsent("test_query", "id_$page", page.toLong(), page.toLong())
            if (page != 10) db.pageMetadataQueries.insertPageMetadataIfAbsent("test_query", page.toLong(), "${page+1}", 20)
        }
        val oldStarted = CompletableDeferred<Job>()
        val recentStarted = CompletableDeferred<Job>()
        val releaseOld = CompletableDeferred<Unit>()
        paginator = StablePaginator(
            serializer = FakeItem.serializer(), queryKey = "test_query",
            config = PaginatorConfig(pageSize = 1, prefetchDistance = 0, maxPagesInDb = 3),
            idExtractor = { it.id }, db = db,
            getPage = { page ->
                if (page == 0) {
                    oldStarted.complete(currentCoroutineContext()[Job]!!)
                    releaseOld.await()
                } else recentStarted.complete(currentCoroutineContext()[Job]!!)
                PagerPayload(20, true, listOf(FakeItem("id_$page", "Fresh")))
            }
        )
        try {
            paginator.onVisiblePageChanged(0)
            val old = oldStarted.await()
            paginator.onVisiblePageChanged(10)
            recentStarted.await().join()
            assertEquals(listOf(6L,7L,8L,9L,10L), db.listingEntryQueries.getExistingPageNumbers("test_query").executeAsList().sorted())
            releaseOld.complete(Unit)
            old.join()
            val remaining = db.listingEntryQueries.getExistingPageNumbers("test_query").executeAsList()
            assertTrue(10L in remaining, "Visible page must survive a late response from a distant page")
            assertEquals(listOf(6L,7L,8L,9L,10L), remaining.sorted())
        } finally {
            releaseOld.complete(Unit)
            paginator.close()
        }
    }

    @Test
    fun test_prefetch_uses_cached_page_but_reset_refreshes_it() = runTest {
        var calls = 0
        paginator = controlledPaginator {
            calls++
            PagerPayload(1, false, listOf(FakeItem("id_0", "Version $calls")))
        }
        paginator.reset(0)
        paginator.itemsMap.first { it[0]?.title == "Version 1" }
        paginator.loadState.first { it == LoadState.END }
        repeat(20) { paginator.onVisiblePageChanged(0) }
        // Reset is ordered after all preceding prefetch commands; only it may perform another fetch.
        paginator.reset(0)
        paginator.itemsMap.first { it[0]?.title == "Version 2" }
        assertEquals(2, calls)
    }


    @Test
    fun test_quick_return_to_page_does_not_let_cancelled_job_remove_replacement() = runTest {
        val requests = kotlinx.coroutines.channels.Channel<Pair<Int, Job>>(kotlinx.coroutines.channels.Channel.UNLIMITED)
        val oldRelease = CompletableDeferred<Unit>()
        val freshRelease = CompletableDeferred<Unit>()
        var zeroCalls = 0
        paginator = controlledPaginator { page ->
            requests.send(page to currentCoroutineContext()[Job]!!)
            if (page != 0) awaitCancellation()
            if (++zeroCalls == 1) withContext(NonCancellable) { oldRelease.await() }
            else freshRelease.await()
            PagerPayload(1, false, listOf(FakeItem("id_0", "Fresh")))
        }
        try {
            paginator.onVisiblePageChanged(0)
            val old = requests.receive().second
            paginator.onVisiblePageChanged(10)
            val distant = requests.receive().second
            paginator.onVisiblePageChanged(0)
            val replacement = requests.receive().second
            oldRelease.complete(Unit)
            old.join()
            assertTrue(replacement.isActive)
            assertTrue(distant.isCancelled)
            freshRelease.complete(Unit)
            replacement.join()
            assertEquals(2, zeroCalls)
            assertEquals(LoadState.END, paginator.loadState.value)
            assertEquals("Fresh", paginator.getItemById("id_0")?.title)
        } finally {
            oldRelease.complete(Unit)
            freshRelease.complete(Unit)
            requests.close()
        }
    }

    @Test
    fun test_many_position_jumps_cancel_obsolete_requests() = runTest {
        val requests = kotlinx.coroutines.channels.Channel<Job>(kotlinx.coroutines.channels.Channel.UNLIMITED)
        val release = CompletableDeferred<Unit>()
        paginator = controlledPaginator { page ->
            requests.send(currentCoroutineContext()[Job]!!)
            release.await()
            PagerPayload(100, true, listOf(FakeItem("id_$page", "Latest")))
        }
        val jobs = mutableListOf<Job>()
        try {
            repeat(50) { page ->
                paginator.onVisiblePageChanged(page)
                jobs.add(requests.receive())
            }
            withContext(Dispatchers.Default) {
                withTimeout(2_000) { jobs.dropLast(1).forEach { it.join() } }
            }
            assertTrue(jobs.dropLast(1).all { it.isCancelled })
            assertTrue(jobs.last().isActive)
            release.complete(Unit)
            jobs.last().join()
            assertEquals(listOf(49L), database.listingEntryQueries.getExistingPageNumbers("test_query").executeAsList())
        } finally {
            release.complete(Unit)
            requests.close()
        }
    }


    @Test
    fun test_jump_to_cached_page_finishes_loading_after_cancelling_old_request() = runTest {
        TestPaginatorFactory.prePopulateDatabase(database, pages = 11, pageSize = 1)
        val started = CompletableDeferred<Job>()
        var calls = 0
        paginator = controlledPaginator {
            calls++
            started.complete(currentCoroutineContext()[Job]!!)
            awaitCancellation()
        }
        paginator.reset(0)
        val old = started.await()
        paginator.onVisiblePageChanged(10)
        old.join()
        withContext(Dispatchers.Default) {
            withTimeout(2_000) { paginator.loadState.first { it == LoadState.END } }
        }
        assertTrue(old.isCancelled)
        assertEquals(1, calls)
    }

    @Test
    fun test_error_retains_original_throwable_and_cached_items() = runTest {
        TestPaginatorFactory.prePopulateDatabase(database, pages = 1, pageSize = 1)
        val originalCause = IllegalStateException("Original cause")
        val failure = object : Throwable("Server message", originalCause) {}
        paginator = controlledPaginator { throw failure }
        val cached = paginator.itemsMap.first { it[0] != null }
        paginator.reset(0)
        val error = paginator.loadState.filterIsInstance<LoadState.ERROR>().first()
        assertSame(failure, error.error)
        assertSame(originalCause, error.error.cause)
        assertEquals("Server message", error.error.message)
        assertEquals(cached, paginator.itemsMap.value)
    }



}
