package io.github.wladyslawpopov.kpage.demo.presentation.screens.settings

import io.github.wladyslawpopov.kpage.demo.domain.cache.CacheRepository
import io.github.wladyslawpopov.kpage.demo.domain.cache.CacheStats
import io.github.wladyslawpopov.kpage.demo.domain.settings.AppLanguage
import io.github.wladyslawpopov.kpage.demo.domain.settings.SettingsRepository
import io.github.wladyslawpopov.kpage.demo.domain.settings.ThemeMode
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceLayout
import io.github.wladyslawpopov.kpage.demo.presentation.screens.settings.display.SettingsOption
import io.github.wladyslawpopov.kpage.demo.presentation.testing.TestTasks
import io.github.wladyslawpopov.kpage.demo.uicore.text.UiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsEngineTest {

    private class FakeSettings : SettingsRepository {
        override val themeMode = MutableStateFlow(ThemeMode.SYSTEM)
        override val language = MutableStateFlow(AppLanguage.ENGLISH)
        override suspend fun spaceLayout() = SpaceLayout.LIST
        override suspend fun setSpaceLayout(layout: SpaceLayout) = Unit
        override suspend fun setThemeMode(mode: ThemeMode) {
            themeMode.value = mode
        }

        override suspend fun setLanguage(language: AppLanguage) {
            this.language.value = language
        }
    }

    private class FakeCache : CacheRepository {
        override val cleared = MutableSharedFlow<Unit>()
        var stats = CacheStats(lists = 3, bytes = 2048)
        var clearCount = 0

        override suspend fun stats() = stats

        override suspend fun clear() {
            clearCount++
            stats = CacheStats(lists = 0, bytes = 0)
        }
    }

    private class Harness(scope: TestScope) {
        val settings = FakeSettings()
        val cache = FakeCache()
        var state = SettingsState()
        val engine = SettingsEngine(
            tasks = TestTasks(
                scope.backgroundScope,
                StandardTestDispatcher(scope.testScheduler),
                StandardTestDispatcher(scope.testScheduler),
            ),
            settings = settings,
            cache = cache,
            getState = { state },
            updateState = { state = it(state) },
            onPageIntent = {},
        )
    }

    private fun <T : Any> List<SettingsOption<T>>.selected() =
        single { it.selected }.value

    @Test
    fun start_shows_the_saved_choices_and_the_cache_numbers() = runTest {
        val harness = Harness(this)
        harness.settings.themeMode.value = ThemeMode.DARK
        harness.settings.language.value = AppLanguage.POLISH

        harness.engine.start()
        runCurrent()

        assertEquals(ThemeMode.DARK, harness.state.themes.selected())
        assertEquals(AppLanguage.POLISH, harness.state.languages.selected())
        assertEquals(UiText.Plain("3"), harness.state.cacheLists)
        assertEquals(UiText.FileSize(2048), harness.state.cacheSize)
    }

    @Test
    fun a_choice_goes_to_the_repository_and_comes_back_through_its_flow() = runTest {
        val harness = Harness(this)
        harness.engine.start()
        runCurrent()

        harness.engine.onIntent(SettingsIntent.SetTheme(ThemeMode.LIGHT))
        harness.engine.onIntent(SettingsIntent.SetLanguage(AppLanguage.RUSSIAN))
        runCurrent()

        assertEquals(ThemeMode.LIGHT, harness.settings.themeMode.value)
        assertEquals(AppLanguage.RUSSIAN, harness.settings.language.value)
        assertEquals(ThemeMode.LIGHT, harness.state.themes.selected())
        assertEquals(AppLanguage.RUSSIAN, harness.state.languages.selected())
    }

    @Test
    fun clearing_the_cache_asks_first_and_does_not_clear_until_confirmed() = runTest {
        val harness = Harness(this)
        harness.engine.start()
        runCurrent()

        harness.engine.onIntent(SettingsIntent.ClearCache)
        runCurrent()
        assertTrue(harness.state.confirmClear)
        assertEquals(0, harness.cache.clearCount)

        harness.engine.onIntent(SettingsIntent.CancelClearCache)
        runCurrent()
        assertFalse(harness.state.confirmClear)
        assertEquals(0, harness.cache.clearCount)
    }

    @Test
    fun a_confirmed_clear_empties_the_cache_and_reads_the_numbers_again() = runTest {
        val harness = Harness(this)
        harness.engine.start()
        runCurrent()

        harness.engine.onIntent(SettingsIntent.ClearCache)
        harness.engine.onIntent(SettingsIntent.ConfirmClearCache)
        runCurrent()

        assertEquals(1, harness.cache.clearCount)
        assertFalse(harness.state.confirmClear)
        assertEquals(UiText.Plain("0"), harness.state.cacheLists)
        assertEquals(UiText.FileSize(0), harness.state.cacheSize)
    }
}
