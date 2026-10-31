package io.github.wladyslawpopov.kpage.core.paging

import io.github.wladyslawpopov.kpage.cache.PagingDatabase
import io.github.wladyslawpopov.kpage.core.paging.data.FakeItem
import io.github.wladyslawpopov.kpage.core.paging.data.GatedServer
import io.github.wladyslawpopov.kpage.core.paging.data.paginator.PaginatorConfig
import io.github.wladyslawpopov.kpage.core.paging.data.state.LoadState
import io.github.wladyslawpopov.kpage.core.paging.data.state.PagingState
import io.github.wladyslawpopov.kpage.core.paging.data.TestPaginatorFactory
import io.github.wladyslawpopov.kpage.core.paging.data.TestPaginatorFactory.QUERY_KEY
import io.github.wladyslawpopov.kpage.core.paging.data.existingPages
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Loading, refresh and showing. The server is held back page by page, so every test controls when
 * answers arrive. The paginator and the database run on the test dispatcher: "wait" means
 * `advanceUntilIdle()` (it also runs out the 500 ms hide grace), so the tests do not depend on time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StablePaginatorTest {

    private companion object {
        const val PAGE = 10
    }

    private lateinit var db: PagingDatabase
    private val paginators = mutableListOf<StablePaginator<FakeItem>>()

    private val pg: StablePaginator<FakeItem> get() = paginators.first()

    @BeforeTest
    fun setup() {
        db = TestPaginatorFactory.createInMemoryDriver()
    }

    @AfterTest
    fun tearDown() {
        paginators.forEach { it.close() }
    }

    // ---- Helpers ----

    private fun ids(count: Int) = List(count) { "id_$it" }

    private fun TestScope.create(
        server: GatedServer,
        config: PaginatorConfig = PaginatorConfig(pageSize = PAGE, initialPageKey = server.firstPage),
    ): StablePaginator<FakeItem> {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return StablePaginator(
            serializer = FakeItem.serializer(),
            queryKey = QUERY_KEY,
            config = config,
            getPage = { server.getPage(it) },
            idExtractor = { it.id },
            db = db,
            scope = backgroundScope,
            dispatcher = testDispatcher,
            databaseDispatcher = testDispatcher,
        ).also { paginators += it }
    }

    /** Puts [pages] into the cache as fully loaded ("Old" titles). */
    private fun seedPages(pages: IntRange, total: Int) {
        db.transaction {
            TestPaginatorFactory.seedQuery(db, QUERY_KEY, total, PAGE)
            for (page in pages) {
                db.pageMetadataQueries.upsertPageMetadata(QUERY_KEY, page.toLong(), (page + 1) * PAGE < total)
                for (i in 0 until PAGE) {
                    val position = page * PAGE + i
                    if (position < total) TestPaginatorFactory.seedItem(db, QUERY_KEY, position, "id_$position")
                }
            }
        }
    }

    /** Runs everything that can run (held pages stay held), then checks the state. */
    private fun TestScope.awaitState(
        paginator: StablePaginator<FakeItem> = pg,
        predicate: (PagingState<FakeItem>) -> Boolean,
    ): PagingState<FakeItem> {
        advanceUntilIdle()
        val state = paginator.state.value
        assertTrue(predicate(state), "Unexpected state: $state")
        return state
    }

    private fun TestScope.awaitRestored() = awaitState { it.isRestored }

    private fun TestScope.awaitSettled() = awaitState { it.isSettled() }

    /** Every event sent before has been handled; "nothing happened" can be checked now. */
    private fun TestScope.barrier() = advanceUntilIdle()

    private fun StablePaginator<FakeItem>.show(first: Int, last: Int = first) = onShown(first..last)

    private fun StablePaginator<FakeItem>.scrollTo(first: Int, last: Int = first) = onVisibleRangeChanged(first..last)

    private fun PagingState<FakeItem>.isSettled() = loadState == LoadState.IDLE || loadState == LoadState.END

    private fun FakeItem?.isOld() = this?.title?.startsWith("Old") == true

    private fun FakeItem?.isNew() = this?.title?.startsWith("New") == true

    // ---- Window ----

    @Test
    fun scrolling_keeps_the_window_and_two_spare_pages() = runTest {
        val server = GatedServer(ids(100), PAGE)
        create(server)

        for (page in 0..6) {
            pg.scrollTo(page * PAGE)
            awaitState { it.items.isLoaded((page + 1) * PAGE) }
        }
        awaitSettled()

        assertEquals(listOf(3L, 4L, 5L, 6L, 7L), db.existingPages(PAGE))
    }

    @Test
    fun a_screen_taller_than_a_page_loads_every_visible_page() = runTest {
        val server = GatedServer(ids(100), PAGE)
        create(server)
        awaitSettled()

        // Rows 0..25 are on screen: pages 0..2 are visible, page 3 is the prefetch.
        pg.show(0, 25)
        val state = awaitState { it.isSettled() }

        assertEquals((0 until 40).toList(), state.items.loadedIndices)
    }

    @Test
    fun loads_outside_the_window_are_cancelled() = runTest {
        val server = GatedServer(ids(100), PAGE)
        create(server)
        awaitState { it.items.isLoaded(19) && it.isSettled() }

        server.hold(2)
        pg.scrollTo(10)
        barrier()
        assertTrue(2 in server.requests)
        pg.scrollTo(80)
        server.release(2)
        awaitState { it.items.isLoaded(70) && it.items.isLoaded(99) && it.isSettled() }

        assertFalse(pg.state.value.items.isLoaded(20), "The answer of the cancelled load is ignored")
        assertFalse(2L in db.existingPages(PAGE))
    }

    @Test
    fun the_first_page_key_is_added_to_requests_but_not_to_positions() = runTest {
        val server = GatedServer(ids(100), PAGE, firstPage = 1)
        create(server)

        val state = awaitState { it.items.isLoaded(19) && it.isSettled() }

        assertEquals(setOf(1, 2), server.requests.toSet())
        assertEquals("id_0", state.items[0]?.id)
        assertEquals("id_19", state.items[19]?.id)
    }

    // ---- Refresh ----

    @Test
    fun refresh_in_the_middle_replaces_exactly_the_window_atomically() = runTest {
        seedPages(0 until 10, total = 100)
        val server = GatedServer(ids(95), PAGE).apply { hold(4, 5, 6) }
        create(server)
        awaitRestored()

        val seen = mutableListOf<PagingState<FakeItem>>()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { pg.state.collect { seen += it } }

        pg.show(50)
        awaitState { it.loadState == LoadState.REFRESH }
        // While the window loads, the old data stays on screen.
        assertTrue(pg.state.value.items[45].isOld())
        assertEquals(100, pg.state.value.totalCount)

        server.release(4, 5, 6)
        val after = awaitState { it.totalCount == 95 && it.items.loadedCount == 30 && it.isSettled() }

        assertEquals((40 until 70).toList(), after.items.loadedIndices)
        assertTrue((40 until 70).all { after.items[it].isNew() })
        assertEquals(listOf(4L, 5L, 6L), db.existingPages(PAGE), "Pages outside the window are dropped")

        collector.cancel()
        seen.forEach { state ->
            val fresh = state.items.loadedIndices.count { state.items[it].isNew() }
            assertTrue(
                fresh == 0 || (fresh == 30 && state.items.loadedCount == 30),
                "Old and new data must never be mixed: fresh=$fresh loaded=${state.items.loadedCount}"
            )
        }
    }

    @Test
    fun refresh_with_a_failing_page_keeps_the_cache_and_reports_an_error() = runTest {
        seedPages(0 until 10, total = 100)
        val server = GatedServer(ids(100), PAGE).apply { failing += 5 }
        create(server)
        awaitRestored()

        pg.show(50)
        val failed = awaitState { it.loadState is LoadState.ERROR }

        assertEquals(100, failed.items.loadedCount)
        assertEquals(100, failed.totalCount)
        assertTrue(failed.items[40].isOld(), "Pages that did arrive must be thrown away")
        assertEquals((0L..9L).toList(), db.existingPages(PAGE))

        server.failing.clear()
        pg.refresh()
        awaitState { it.items[50].isNew() && it.isSettled() }
    }

    @Test
    fun retrying_after_a_network_error_keeps_the_position_and_the_cache() = runTest {
        seedPages(0 until 10, total = 100)
        val server = GatedServer(ids(100), PAGE).apply { failAll = true }
        create(server)
        awaitRestored()
        pg.scrollTo(50)

        pg.refresh()
        val failed = awaitState { it.loadState is LoadState.ERROR }
        assertEquals(100, failed.items.loadedCount, "The cache is intact")

        server.failAll = false
        server.clearRequests()
        pg.refresh()
        val state = awaitState { it.items[50].isNew() && it.isSettled() }

        assertEquals(listOf(4, 5, 6), server.requests.sorted(), "The retry refreshes where the user is")
        assertEquals((40 until 70).toList(), state.items.loadedIndices)
    }

    @Test
    fun scrolling_out_of_the_window_during_refresh_cancels_it() = runTest {
        seedPages(0 until 10, total = 100)
        val server = GatedServer(ids(100), PAGE).apply { hold(4, 5, 6) }
        create(server)
        awaitRestored()

        pg.show(50)
        awaitState { it.loadState == LoadState.REFRESH }
        pg.scrollTo(80)
        server.release(4, 5, 6)

        val state = awaitSettled()
        assertEquals(100, state.items.loadedCount)
        assertTrue(state.items[50].isOld(), "Answers of the cancelled refresh must be ignored")
    }

    @Test
    fun a_shorter_list_moves_the_refresh_to_its_new_end_even_if_pages_past_the_end_fail() = runTest {
        seedPages(0 until 10, total = 100)
        val server = GatedServer(ids(35), PAGE).apply { failing += 9; hold(8) }
        create(server)
        awaitRestored()

        pg.show(90)
        // Page 9 is past the new end and fails; page 8 (empty, total 35) reveals the new end.
        barrier()
        assertTrue(server.requests.containsAll(listOf(8, 9)))
        server.release(8)
        val state = awaitState { it.totalCount == 35 && it.items.isLoaded(34) && it.isSettled() }

        assertEquals(LoadState.END, state.loadState, "The failure past the new end is not an error")
        assertEquals(listOf(2, 3, 8, 9), server.requests.sorted(), "Then around the new last page, within the new total")
        assertEquals((20 until 35).toList(), state.items.loadedIndices)
    }

    @Test
    fun a_failure_of_a_page_past_the_exact_total_does_not_fail_the_refresh() = runTest {
        seedPages(0 until 10, total = 100)
        // The list has 45 items now; page 5 (50..59) is past the end and the server answers 404.
        val server = GatedServer(ids(45), PAGE).apply { failing += 5 }
        create(server)
        awaitRestored()

        pg.show(40)
        val state = awaitState { it.totalCount == 45 && it.isSettled() }

        assertEquals(LoadState.END, state.loadState)
        assertEquals((30 until 45).toList(), state.items.loadedIndices)
    }

    @Test
    fun removal_on_the_server_then_refresh_leaves_no_holes() = runTest {
        seedPages(0 until 10, total = 100)
        val server = GatedServer(ids(100) - "id_45" - "id_46", PAGE)
        create(server)
        awaitRestored()

        pg.scrollTo(50)
        pg.refresh()
        val state = awaitState { it.totalCount == 98 && it.isSettled() }

        assertEquals((40 until 70).toList(), state.items.loadedIndices)
        assertEquals("id_47", state.items[45]?.id)
        assertEquals(listOf(4L, 5L, 6L), db.existingPages(PAGE), "Shifted pages outside the window are dropped")
    }

    // ---- Showing and returning ----

    @Test
    fun the_first_show_refreshes_around_the_restored_anchor_not_from_zero() = runTest {
        seedPages(0 until 10, total = 100)
        val server = GatedServer(ids(100), PAGE)
        create(server)
        awaitRestored()

        pg.show(70)
        awaitState { it.items[75].isNew() && it.isSettled() }

        assertEquals(listOf(6, 7, 8), server.requests.sorted())
    }

    @Test
    fun returning_to_the_list_refreshes_it() = runTest {
        seedPages(0 until 10, total = 100)
        val server = GatedServer(ids(100), PAGE)
        create(server)
        awaitRestored()

        pg.show(20)
        awaitState { it.items[20].isNew() && it.isSettled() }
        server.clearRequests()
        server.version = "Newer"

        pg.onHidden()
        barrier() // longer than the hide grace
        pg.show(20)
        awaitState { it.items[20]?.title?.startsWith("Newer") == true && it.isSettled() }

        assertEquals(listOf(1, 2, 3), server.requests.sorted())
    }

    @Test
    fun a_fast_hide_and_show_like_a_tab_switch_does_not_refresh() = runTest {
        seedPages(0 until 10, total = 100)
        val server = GatedServer(ids(100), PAGE)
        create(server)
        awaitRestored()
        pg.show(20)
        awaitSettled()
        server.clearRequests()

        pg.onHidden()
        pg.show(20)
        barrier()

        assertTrue(server.requests.isEmpty())
        assertTrue(pg.state.value.isSettled())
    }

    @Test
    fun an_empty_cache_loads_by_itself_and_the_first_show_does_not_repeat_it() = runTest {
        val server = GatedServer(ids(100), PAGE).apply { hold(0, 1) }
        create(server)

        awaitState { it.loadState == LoadState.INITIAL }
        server.release(0, 1)
        awaitState { it.items.isLoaded(19) && it.isSettled() }
        pg.show(0)
        barrier()

        assertEquals(listOf(0, 1), server.requests.sorted())
        assertTrue(pg.state.value.isSettled())
    }

    @Test
    fun returning_to_an_empty_list_refreshes_it() = runTest {
        val server = GatedServer(emptyList(), PAGE)
        create(server)
        awaitState { it.loadState == LoadState.END }
        pg.onShown(null)
        barrier()

        // Something appeared on the server while the user was away.
        server.ids = ids(5)
        pg.onHidden()
        barrier()
        pg.onShown(null)
        val state = awaitState { it.isSettled() }

        assertEquals(5, state.totalCount)
        assertEquals((0 until 5).toList(), state.items.loadedIndices)
    }

    @Test
    fun a_list_shown_before_its_cache_reached_the_ui_waits_for_its_rows() = runTest {
        seedPages(0 until 10, total = 100)
        val server = GatedServer(ids(100), PAGE)
        create(server)

        // The UI has no rows yet: nothing is requested; the rows then bring the real first show.
        pg.onShown(null)
        barrier()
        assertTrue(server.requests.isEmpty())

        pg.show(70)
        awaitSettled()
        assertEquals(listOf(6, 7, 8), server.requests.sorted())
    }

    @Test
    fun a_refresh_over_the_cache_shows_REFRESH() = runTest {
        seedPages(0 until 10, total = 100)
        val server = GatedServer(ids(100), PAGE).apply { hold(0, 1) }
        create(server)
        awaitRestored()

        pg.refresh()
        awaitState { it.loadState == LoadState.REFRESH }
        server.release(0, 1)
        awaitState { it.items[0].isNew() && it.isSettled() }
    }

    @Test
    fun an_empty_list_reaches_END_by_itself() = runTest {
        val server = GatedServer(emptyList(), PAGE)
        create(server)

        val state = awaitState { it.loadState == LoadState.END }

        assertEquals(0, state.totalCount)
        assertEquals(0, state.items.size)
    }

    @Test
    fun a_cold_start_without_network_shows_the_cache_and_the_total() = runTest {
        seedPages(0 until 10, total = 100)
        val server = GatedServer(ids(100), PAGE).apply { failAll = true }
        create(server)

        val restored = awaitRestored()
        assertEquals(100, restored.totalCount)
        assertEquals(100, restored.items.loadedCount)

        pg.show(50)
        val failed = awaitState { it.loadState is LoadState.ERROR }
        assertEquals(100, failed.items.loadedCount)
    }

    // ---- A list without a total count ----

    @Test
    fun a_list_without_a_total_grows_as_the_user_nears_its_end() = runTest {
        val server = GatedServer(ids(25), PAGE).apply { reportsTotal = false }
        create(server)

        val first = awaitSettled()
        assertEquals(20, first.totalCount, "Only the loaded part, no placeholders after it")
        assertEquals(LoadState.IDLE, first.loadState, "The end is not known yet")

        pg.scrollTo(10, 19)
        val state = awaitSettled()

        assertEquals(25, state.totalCount)
        assertEquals(LoadState.END, state.loadState, "A short page is the end")
        assertEquals((0 until 25).toList(), state.items.loadedIndices)
    }

    // ---- Several paginators ----

    @Test
    fun two_live_paginators_of_the_same_list_keep_their_own_pages() = runTest {
        seedPages(0 until 10, total = 100)
        val server = GatedServer(ids(100), PAGE)
        val first = create(server)
        awaitRestored()
        first.show(0)
        awaitSettled()

        // The same list opened on a second screen, far below.
        val second = create(server)
        second.show(80)
        awaitState(second) { it.items.isLoaded(85) && it.isSettled() }

        val state = awaitState(first) { it.isSettled() }
        assertTrue((0 until 20).all { state.items.isLoaded(it) }, "The second screen did not delete the first one's pages")
    }

    // ---- Changing one item ----

    @Test
    fun updateItem_during_a_refresh_is_not_overwritten_by_an_older_answer() = runTest {
        seedPages(0 until 10, total = 100)
        val server = GatedServer(ids(100), PAGE).apply { hold(4, 5, 6) }
        create(server)
        awaitRestored()

        pg.show(50)
        awaitState { it.loadState == LoadState.REFRESH }
        pg.updateItem("id_50") { it.copy(title = "Edited") }
        server.release(4, 5, 6)

        val state = awaitState { it.items.loadedCount == 30 && it.isSettled() }
        assertEquals("Edited", state.items[50]?.title)
        assertTrue(state.items[51].isNew())
    }

    @Test
    fun updateItem_returns_null_for_an_item_that_is_not_cached() = runTest {
        seedPages(0 until 1, total = 10)
        create(GatedServer(ids(10), PAGE))
        awaitRestored()

        assertEquals(null, pg.updateItem("missing") { it.copy(title = "x") })
        assertEquals("New", pg.updateItem("id_3") { it.copy(title = "New") }?.title)
    }
}
