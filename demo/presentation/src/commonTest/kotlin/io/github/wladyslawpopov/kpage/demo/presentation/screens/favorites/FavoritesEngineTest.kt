package io.github.wladyslawpopov.kpage.demo.presentation.screens.favorites

import io.github.wladyslawpopov.kpage.core.paging.data.paginator.Paginator
import io.github.wladyslawpopov.kpage.demo.domain.favorites.FavoritesRepository
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceImage
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageIntent
import io.github.wladyslawpopov.kpage.demo.presentation.testing.FakePaginator
import io.github.wladyslawpopov.kpage.demo.presentation.testing.TestTasks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

        override fun favorites(scope: CoroutineScope): Paginator<SpaceImage> =
            FakePaginator<SpaceImage>().also { paginators += it }

        override suspend fun setFavorite(id: String, isFavorite: Boolean) = Unit

        override suspend fun removeFavorites(ids: Collection<String>) {
            removed += ids
        }
    }

    private class Harness(scope: TestScope) {
        val favorites = FakeFavorites()
        val intents = mutableListOf<PageIntent>()
        var state = FavoritesState()
        val engine = FavoritesEngine(
            tasks = TestTasks(
                scope.backgroundScope,
                StandardTestDispatcher(scope.testScheduler),
                StandardTestDispatcher(scope.testScheduler),
            ),
            favorites = favorites,
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
    fun a_confirmed_delete_removes_only_the_confirmed_ids() = runTest {
        val harness = started()
        harness.engine.onIntent(FavoritesIntent.LongClick("a"))
        harness.engine.onIntent(FavoritesIntent.LongClick("b"))
        harness.engine.onIntent(FavoritesIntent.DeleteOne("a"))

        harness.engine.onIntent(FavoritesIntent.ConfirmDelete)
        runCurrent()

        assertEquals(listOf<Collection<String>>(setOf("a")), harness.favorites.removed)
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
