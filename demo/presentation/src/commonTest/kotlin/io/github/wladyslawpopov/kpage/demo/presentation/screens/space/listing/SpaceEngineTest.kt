package io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing

import io.github.wladyslawpopov.kpage.core.paging.data.paginator.Paginator
import io.github.wladyslawpopov.kpage.demo.domain.cache.CacheRepository
import io.github.wladyslawpopov.kpage.demo.domain.cache.CacheStats
import io.github.wladyslawpopov.kpage.demo.domain.favorites.FavoritesRepository
import io.github.wladyslawpopov.kpage.demo.domain.settings.AppLanguage
import io.github.wladyslawpopov.kpage.demo.domain.settings.SettingsRepository
import io.github.wladyslawpopov.kpage.demo.domain.settings.ThemeMode
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceCenter
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceImage
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceImageDetails
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceLayout
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceQuery
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceRepository
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageIntent
import io.github.wladyslawpopov.kpage.demo.presentation.testing.FakePaginator
import io.github.wladyslawpopov.kpage.demo.presentation.testing.TestTasks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class SpaceEngineTest {

    private class FakeSpace : SpaceRepository {
        val created = mutableListOf<Pair<SpaceQuery, FakePaginator<SpaceImage>>>()

        override fun images(query: SpaceQuery, scope: CoroutineScope): Paginator<SpaceImage> =
            FakePaginator<SpaceImage>().also { created += query to it }

        override suspend fun details(id: String): SpaceImageDetails = error("not used")
        override suspend fun updateImage(id: String, transform: (SpaceImage) -> SpaceImage) = Unit
    }

    private class FakeFavorites : FavoritesRepository {
        val set = mutableListOf<Pair<String, Boolean>>()
        override val changes = MutableSharedFlow<Unit>()
        override fun favorites(scope: CoroutineScope): Paginator<SpaceImage> = error("not used")
        override suspend fun setFavorite(id: String, isFavorite: Boolean) {
            set += id to isFavorite
        }

        override suspend fun removeFavorites(ids: Collection<String>) = Unit
    }

    private class FakeSettings : SettingsRepository {
        override val themeMode = MutableStateFlow(ThemeMode.SYSTEM)
        override val language = MutableStateFlow(AppLanguage.ENGLISH)
        var layout = SpaceLayout.LIST
        override suspend fun spaceLayout() = layout
        override suspend fun setSpaceLayout(layout: SpaceLayout) {
            this.layout = layout
        }

        override suspend fun setThemeMode(mode: ThemeMode) = Unit
        override suspend fun setLanguage(language: AppLanguage) = Unit
    }

    private class FakeCache : CacheRepository {
        override val cleared = MutableSharedFlow<Unit>()
        override suspend fun stats() = CacheStats(0, 0)
        override suspend fun clear() = Unit
    }

    private class Harness(scope: TestScope, val initialQuery: SpaceQuery = SpaceQuery()) {
        val space = FakeSpace()
        val favorites = FakeFavorites()
        val settings = FakeSettings()
        val intents = mutableListOf<PageIntent>()
        val state = MutableStateFlow(SpaceState(list = SpaceState().list.copy(query = initialQuery), searchText = initialQuery.text))
        val engine = SpaceEngine(
            tasks = TestTasks(
                scope.backgroundScope,
                StandardTestDispatcher(scope.testScheduler),
                StandardTestDispatcher(scope.testScheduler),
            ),
            initialQuery = initialQuery,
            repository = space,
            favorites = favorites,
            settings = settings,
            cache = FakeCache(),
            states = state,
            getState = { state.value },
            updateState = { state.value = it(state.value) },
            onPageIntent = { intents += it },
        )

        val queries get() = space.created.map { it.first }
        val paginator get() = space.created.last().second
    }

    private fun TestScope.started(initialQuery: SpaceQuery = SpaceQuery()) = Harness(this, initialQuery).also {
        it.engine.start()
        runCurrent()
    }

    private fun TestScope.pass(millis: Long) {
        advanceTimeBy(millis.milliseconds)
        runCurrent()
    }

    @Test
    fun the_list_follows_the_text_only_after_a_pause_in_typing() = runTest {
        val harness = started()

        harness.engine.onIntent(SpaceIntent.SearchChanged("m"))
        pass(200)
        harness.engine.onIntent(SpaceIntent.SearchChanged("moon"))
        pass(399)
        assertEquals(listOf(SpaceQuery()), harness.queries)

        pass(1)
        assertEquals(listOf(SpaceQuery(), SpaceQuery(text = "moon")), harness.queries)
    }

    @Test
    fun the_text_is_trimmed_and_a_change_of_spaces_alone_does_not_reload() = runTest {
        val harness = started()
        harness.engine.onIntent(SpaceIntent.SearchChanged("  moon "))
        pass(400)
        assertEquals(SpaceQuery(text = "moon"), harness.queries.last())

        harness.engine.onIntent(SpaceIntent.SearchChanged("moon  "))
        pass(400)

        assertEquals(2, harness.queries.size)
    }

    @Test
    fun the_filters_apply_at_once_and_keep_the_text() = runTest {
        val harness = started()
        harness.engine.onIntent(SpaceIntent.SearchChanged("moon"))
        pass(400)

        harness.engine.onIntent(SpaceIntent.SetFilters(SpaceCenter.JPL, yearStart = 1969, yearEnd = null, tag = null))
        runCurrent()

        assertEquals(SpaceQuery(text = "moon", center = SpaceCenter.JPL, yearStart = 1969), harness.queries.last())
    }

    @Test
    fun a_new_query_clears_the_error_of_the_page() = runTest {
        val harness = started()

        harness.engine.onIntent(SpaceIntent.SetFilters(SpaceCenter.HQ, null, null, null))
        runCurrent()

        assertTrue(PageIntent.ClearError in harness.intents)
    }

    @Test
    fun a_reset_brings_back_the_query_and_the_text_the_list_was_opened_with() = runTest {
        val opened = SpaceQuery(text = "mars")
        val harness = started(opened)
        harness.engine.onIntent(SpaceIntent.SetFilters(SpaceCenter.JPL, null, null, null))
        runCurrent()

        harness.engine.onReset()
        runCurrent()

        assertEquals(opened, harness.queries.last())
        assertEquals("mars", harness.state.value.searchText)
    }

    @Test
    fun nothing_found_offers_a_way_back_only_when_the_query_was_changed() = runTest {
        val harness = started()
        harness.paginator.recover()
        runCurrent()
        assertNotNull(harness.state.value.page.empty)
        assertEquals(PageIntent.Refresh, harness.state.value.page.empty?.action)

        harness.engine.onIntent(SpaceIntent.SetFilters(SpaceCenter.JPL, null, null, null))
        runCurrent()
        harness.paginator.recover()
        runCurrent()

        assertEquals(PageIntent.Reset, harness.state.value.page.empty?.action)
    }

    @Test
    fun the_layout_switches_at_once_and_is_saved() = runTest {
        val harness = started()

        harness.engine.onIntent(SpaceIntent.ToggleLayout)
        runCurrent()

        assertEquals(SpaceLayout.GRID, harness.state.value.layout)
        assertEquals(SpaceLayout.GRID, harness.settings.layout)
    }

    @Test
    fun the_saved_layout_is_restored_on_start() = runTest {
        val harness = Harness(this)
        harness.settings.layout = SpaceLayout.GRID

        harness.engine.start()
        runCurrent()

        assertEquals(SpaceLayout.GRID, harness.state.value.layout)
    }

    @Test
    fun a_heart_is_saved_and_then_applied_to_the_cached_item() = runTest {
        val harness = started()

        harness.engine.onIntent(SpaceIntent.SetFavorite("a", isFavorite = true))
        runCurrent()

        assertEquals(listOf("a" to true), harness.favorites.set)
        assertEquals(listOf("a"), harness.paginator.updatedIds)
    }
}
