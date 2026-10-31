package io.github.wladyslawpopov.kpage.core.paging

import io.github.wladyslawpopov.kpage.cache.PagingDatabase
import io.github.wladyslawpopov.kpage.core.paging.data.DEFAULT_MAX_LISTS
import io.github.wladyslawpopov.kpage.core.paging.data.FakeItem
import io.github.wladyslawpopov.kpage.core.paging.data.GatedServer
import io.github.wladyslawpopov.kpage.core.paging.data.OtherItem
import io.github.wladyslawpopov.kpage.core.paging.data.TestPaginatorFactory
import io.github.wladyslawpopov.kpage.core.paging.data.TestPaginatorFactory.ITEM_TYPE
import io.github.wladyslawpopov.kpage.core.paging.data.TestPaginatorFactory.QUERY_KEY
import io.github.wladyslawpopov.kpage.core.paging.data.cache.cacheStats
import io.github.wladyslawpopov.kpage.core.paging.data.cache.maxLists
import io.github.wladyslawpopov.kpage.core.paging.data.cache.setMaxLists
import io.github.wladyslawpopov.kpage.core.paging.data.paginator.PagerPayload
import io.github.wladyslawpopov.kpage.core.paging.data.paginator.PaginatorConfig
import io.github.wladyslawpopov.kpage.core.paging.data.state.LoadState
import io.github.wladyslawpopov.kpage.core.paging.data.state.PagingState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The position-addressed cache: shifts, broken records, item types, shared items and eviction. */
@OptIn(ExperimentalCoroutinesApi::class)
class PagingModelTest {

    private lateinit var database: PagingDatabase
    private val paginators = mutableListOf<StablePaginator<*>>()

    @BeforeTest
    fun setup() {
        database = TestPaginatorFactory.createInMemoryDriver()
    }

    @AfterTest
    fun tearDown() {
        paginators.forEach { it.close() }
    }

    /** A paginator on the test dispatcher, for the paginator and the database alike. */
    private fun <T : Any> TestScope.paginator(
        serializer: KSerializer<T>,
        config: PaginatorConfig,
        queryKey: String = QUERY_KEY,
        idExtractor: (T) -> String,
        getPage: suspend (Int) -> PagerPayload<T>,
    ): StablePaginator<T> {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return StablePaginator(
            serializer = serializer,
            queryKey = queryKey,
            config = config,
            getPage = getPage,
            idExtractor = idExtractor,
            db = database,
            scope = backgroundScope,
            dispatcher = testDispatcher,
            databaseDispatcher = testDispatcher,
        ).also { paginators += it }
    }

    /** A paginator that never reaches the network: whatever it shows comes from the database. */
    private fun TestScope.offlinePaginator(
        pageSize: Int,
        queryKey: String = QUERY_KEY,
    ) = paginator(
        serializer = FakeItem.serializer(),
        config = PaginatorConfig(
            pageSize = pageSize, prefetchDistance = 0,
        ),
        queryKey = queryKey,
        idExtractor = { it.id },
        getPage = { throw IllegalStateException("offline") },
    )

    /** Runs everything that can run, then checks the state. */
    private fun <T : Any> TestScope.awaitState(
        paginator: StablePaginator<T>,
        predicate: (PagingState<T>) -> Boolean,
    ): PagingState<T> {
        advanceUntilIdle()
        val state = paginator.state.value
        assertTrue(predicate(state), "Unexpected state: $state")
        return state
    }

    private fun PagingState<FakeItem>.ids(range: IntRange) = range.map { items.getOrNull(it)?.id }

    @Test
    fun test_server_shift_leaves_no_holes() = runTest {
        val server = GatedServer((0..8).map { "id_$it" }, pageSize = 3)
        val paginator = paginator(
            serializer = FakeItem.serializer(),
            config = PaginatorConfig(pageSize = 3, prefetchDistance = 0),
            idExtractor = { it.id },
            getPage = { server.getPage(it) },
        )
        awaitState(paginator) { it.items.isLoaded(2) && it.loadState == LoadState.IDLE }
        paginator.onVisibleRangeChanged(3..3)
        awaitState(paginator) { it.items.isLoaded(5) && it.loadState == LoadState.IDLE }

        // A new item appears at the top: every cached item is now one position further on the server.
        server.ids = listOf("id_new") + server.ids
        paginator.onVisibleRangeChanged(6..6)
        val state = awaitState(paginator) { it.items.isLoaded(8) }

        assertEquals(10, state.totalCount)
        assertEquals(
            listOf("id_0", "id_1", "id_2", "id_3", "id_4", "id_5", "id_5", "id_6", "id_7"),
            state.ids(0..8),
            "Positions 6..8 hold the new page; id_5 is duplicated on the seam instead of leaving a hole",
        )
    }

    @Test
    fun test_broken_json_does_not_shift_neighbours() = runTest {
        TestPaginatorFactory.prePopulateDatabase(database, pages = 1, pageSize = 5)
        database.itemRawQueries.upsertItem(ITEM_TYPE, "id_2", "{broken")
        val paginator = offlinePaginator(pageSize = 5)

        val state = awaitState(paginator) { it.isRestored }

        assertEquals(5, state.totalCount)
        assertNull(state.items[2])
        assertFalse(state.items.isLoaded(2))
        assertEquals(listOf("id_0", "id_1", null, "id_3", "id_4"), state.ids(0..4))
        assertEquals(4, state.items.loadedCount)
    }

    @Test
    fun test_same_id_of_two_types_in_two_lists_does_not_collide() = runTest {
        val first = paginator(
            serializer = FakeItem.serializer(), queryKey = "list_a",
            config = PaginatorConfig(pageSize = 1, prefetchDistance = 0),
            idExtractor = { it.id },
            getPage = { PagerPayload(1, false, listOf(FakeItem("x", "fake"))) },
        )
        val second = paginator(
            serializer = OtherItem.serializer(), queryKey = "list_b",
            config = PaginatorConfig(pageSize = 1, prefetchDistance = 0),
            idExtractor = { it.id },
            getPage = { PagerPayload(1, false, listOf(OtherItem("x", "other"))) },
        )

        awaitState(first) { it.items.getOrNull(0)?.title == "fake" }
        awaitState(second) { it.items.getOrNull(0)?.title == "other" }

        assertEquals("fake", first.getItemById("x")?.title)
        assertEquals("other", second.getItemById("x")?.title)
    }

    @Test
    fun test_update_item_changes_every_position_in_every_list() = runTest {
        listOf("list_a", "list_b").forEach { queryKey ->
            TestPaginatorFactory.seedQuery(database, queryKey, totalCount = 3, pageSize = 3)
            listOf("A", "B", "A").forEachIndexed { position, id ->
                TestPaginatorFactory.seedItem(database, queryKey, position, id, TestPaginatorFactory.itemJson(id, "Old"))
            }
        }
        val first = offlinePaginator(pageSize = 3, queryKey = "list_a")
        val second = offlinePaginator(pageSize = 3, queryKey = "list_b")
        awaitState(first) { it.isRestored }
        awaitState(second) { it.isRestored }

        first.updateItem("A") { it.copy(title = "New") }

        val inFirst = awaitState(first) { it.items.getOrNull(0)?.title == "New" }
        val inSecond = awaitState(second) { it.items.getOrNull(0)?.title == "New" }
        assertEquals(listOf("New", "Old", "New"), (0..2).map { inFirst.items[it]?.title })
        assertEquals(listOf("New", "Old", "New"), (0..2).map { inSecond.items[it]?.title })
    }

    private fun seedList(queryKey: String) {
        TestPaginatorFactory.seedQuery(database, queryKey, totalCount = 1, pageSize = 1)
        TestPaginatorFactory.seedItem(database, queryKey, 0, "${queryKey}_item")
    }

    @Test
    fun test_creating_a_list_beyond_the_limit_evicts_the_oldest() = runTest {
        assertEquals(100, DEFAULT_MAX_LISTS)
        val onTest = StandardTestDispatcher(testScheduler)
        database.setMaxLists(10, onTest)
        for (i in 1..11) {
            seedList("q$i")
            offlinePaginator(pageSize = 1, queryKey = "q$i").awaitInitialized()
        }

        assertNull(database.queryMetaQueries.selectByKey("q1").executeAsOneOrNull())
        assertEquals(0L, database.listingEntryQueries.countEntries("q1").executeAsOne())
        assertNull(database.itemRawQueries.selectById(ITEM_TYPE, "q1_item").executeAsOneOrNull())
        for (i in 2..11) {
            assertNotNull(database.queryMetaQueries.selectByKey("q$i").executeAsOneOrNull(), "q$i must stay")
            assertNotNull(database.itemRawQueries.selectById(ITEM_TYPE, "q${i}_item").executeAsOneOrNull())
        }
        assertEquals(10, database.cacheStats(onTest).lists)
        assertEquals(10, database.cacheStats(onTest).items)

        // A lowered limit evicts at once.
        database.setMaxLists(3, onTest)
        assertEquals(3, database.cacheStats(onTest).lists)
        assertEquals(3, database.maxLists(onTest))
    }

    @Test
    fun test_changed_page_size_drops_the_cache_of_that_query() = runTest {
        TestPaginatorFactory.prePopulateDatabase(database, pages = 1, pageSize = 20)

        offlinePaginator(pageSize = 10).awaitInitialized()

        assertEquals(0L, database.listingEntryQueries.countEntries(QUERY_KEY).executeAsOne())
        val meta = database.queryMetaQueries.selectByKey(QUERY_KEY).executeAsOne()
        assertEquals(10L, meta.pageSize)
        assertEquals(0L, meta.totalCount)
        assertNull(database.itemRawQueries.selectById(ITEM_TYPE, "id_0").executeAsOneOrNull())
    }
}
