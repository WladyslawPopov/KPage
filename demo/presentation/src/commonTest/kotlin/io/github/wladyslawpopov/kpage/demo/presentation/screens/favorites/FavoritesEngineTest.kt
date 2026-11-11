package io.github.wladyslawpopov.kpage.demo.presentation.screens.favorites

import io.github.wladyslawpopov.kpage.core.paging.data.paginator.Paginator
import io.github.wladyslawpopov.kpage.demo.domain.cache.CacheRepository
import io.github.wladyslawpopov.kpage.demo.domain.cache.CacheStats
import io.github.wladyslawpopov.kpage.demo.domain.favorites.FavoritesRepository
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceImage
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceImageDetails
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceQuery
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceRepository
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageIntent
import io.github.wladyslawpopov.kpage.demo.presentation.testing.FakePaginator
import io.github.wladyslawpopov.kpage.demo.presentation.testing.TestTasks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesEngineTest {

    private class FakeFavorites : FavoritesRepository {
        val paginators = mutableListOf<FakePaginator<SpaceImage>>()
        val removed = mutableListOf<Collection<String>>()
        override val changes = MutableSharedFlow<Unit>()

        override fun favorites(scope: CoroutineScope): Paginator<SpaceImage> =
            FakePaginator<SpaceImage>().also { paginators += it }

        override suspend fun setFavorite(id: String, isFavorite: Boolean) = Unit

        override suspend fun removeFavorites(ids: Collection<String>) {
            removed += ids
        }
    }

    private class FakeSpace : SpaceRepository {
        val updated = mutableMapOf<String, (SpaceImage) -> SpaceImage>()

        override fun images(query: SpaceQuery, scope: CoroutineScope): Paginator<SpaceImage> = error("not used")
        override suspend fun details(id: String): SpaceImageDetails? = error("not used")

        override suspend fun updateImage(id: String, transform: (SpaceImage) -> SpaceImage) {
            updated[id] = transform
        }
    }

    private class FakeCache : CacheRepository {
        override val cleared = MutableSharedFlow<Unit>()
        override suspend fun stats() = CacheStats(0, 0)
        override suspend fun clear() = Unit
    }

    private class Harness(scope: TestScope) {
        val favorites = FakeFavorites()
        val space = FakeSpace()
        val cache = FakeCache()
        val intents = mutableListOf<PageIntent>()
        var state = FavoritesState()
        val engine = FavoritesEngine(
            tasks = TestTasks(
                scope.backgroundScope,
                StandardTestDispatcher(scope.testScheduler),
                StandardTestDispatcher(scope.testScheduler),
            ),
            favorites = favorites,
            space = space,
            cache = cache,
            getState = { state },
            updateState = { state = it(state) },
            onPageIntent = { intents += it },
        )
        val paginator get() = favorites.paginators.last()
    }

    private fun TestScope.started() = Harness(this).also {
        it.engine.start()
        runCurrent()
    }

    @Test
    fun a_change_of_the_favorites_refreshes_the_list() = runTest {
        val harness = started()

        harness.favorites.changes.emit(Unit)
        runCurrent()

        assertEquals(1, harness.paginator.refreshCount)
    }

    @Test
    fun a_cleared_cache_refreshes_the_list() = runTest {
        val harness = started()

        harness.cache.cleared.emit(Unit)
        runCurrent()

        assertEquals(1, harness.paginator.refreshCount)
    }

    @Test
    fun a_tap_selects_only_after_a_long_press_started_the_selection() = runTest {
        val harness = started()

        harness.engine.onIntent(FavoritesIntent.Click("a"))
        assertTrue(harness.state.selected.isEmpty())

        harness.engine.onIntent(FavoritesIntent.LongClick("a"))
        harness.engine.onIntent(FavoritesIntent.Click("b"))
        assertEquals(setOf("a", "b"), harness.state.selected)
        assertTrue(harness.state.isSelecting)

        harness.engine.onIntent(FavoritesIntent.Click("a"))
        assertEquals(setOf("b"), harness.state.selected)

        harness.engine.onIntent(FavoritesIntent.ClearSelection)
        assertFalse(harness.state.isSelecting)
    }

    @Test
    fun deleting_asks_first_and_touches_nothing_until_confirmed() = runTest {
        val harness = started()
        harness.engine.onIntent(FavoritesIntent.LongClick("a"))
        harness.engine.onIntent(FavoritesIntent.LongClick("b"))

        harness.engine.onIntent(FavoritesIntent.DeleteSelected)
        runCurrent()
        assertEquals(setOf("a", "b"), harness.state.pendingDelete)
        assertTrue(harness.favorites.removed.isEmpty())

        harness.engine.onIntent(FavoritesIntent.CancelDelete)
        runCurrent()
        assertTrue(harness.state.pendingDelete.isEmpty())
        assertTrue(harness.favorites.removed.isEmpty())
        assertEquals(setOf("a", "b"), harness.state.selected)
    }

    @Test
    fun deleting_with_nothing_chosen_asks_nothing() = runTest {
        val harness = started()

        harness.engine.onIntent(FavoritesIntent.DeleteSelected)

        assertTrue(harness.state.pendingDelete.isEmpty())
    }

    @Test
    fun a_confirmed_delete_removes_the_ids_and_clears_the_heart_in_the_space_lists() = runTest {
        val harness = started()
        harness.engine.onIntent(FavoritesIntent.LongClick("a"))
        harness.engine.onIntent(FavoritesIntent.LongClick("b"))
        harness.engine.onIntent(FavoritesIntent.DeleteOne("a"))

        harness.engine.onIntent(FavoritesIntent.ConfirmDelete)
        runCurrent()

        assertEquals(listOf<Collection<String>>(setOf("a")), harness.favorites.removed)
        assertEquals(setOf("a"), harness.space.updated.keys)
        val image = SpaceImage(id = "a", title = "A", previewUrl = null, date = null, center = null, isFavorite = true)
        assertFalse(harness.space.updated.getValue("a")(image).isFavorite)
        assertTrue(harness.state.pendingDelete.isEmpty())
        assertEquals(setOf("b"), harness.state.selected)
    }

    @Test
    fun an_empty_result_shows_the_empty_state_and_a_first_load_shows_the_loader() = runTest {
        val harness = started()
        assertTrue(harness.state.page.isLoading)
        assertNull(harness.state.page.empty)

        harness.paginator.recover()
        runCurrent()

        assertFalse(harness.state.page.isLoading)
        assertNotNull(harness.state.page.empty)
    }

    @Test
    fun a_load_error_goes_to_the_page_once() = runTest {
        val harness = started()
        val error = IllegalStateException("offline")

        harness.paginator.fail(error)
        runCurrent()

        assertEquals(listOf<PageIntent>(PageIntent.OnError(error)), harness.intents)
    }
}
