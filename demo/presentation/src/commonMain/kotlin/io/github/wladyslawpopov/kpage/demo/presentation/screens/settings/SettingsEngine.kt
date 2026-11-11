package io.github.wladyslawpopov.kpage.demo.presentation.screens.settings

import io.github.wladyslawpopov.kpage.demo.domain.cache.CacheRepository
import io.github.wladyslawpopov.kpage.demo.domain.settings.SettingsRepository
import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentTasks
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageIntent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.settings.options.SettingsOptions
import io.github.wladyslawpopov.kpage.demo.uicore.text.UiText

/** Business scenarios of the Settings tab. */
internal class SettingsEngine(
    private val tasks: ComponentTasks,
    private val settings: SettingsRepository,
    private val cache: CacheRepository,
    private val getState: () -> SettingsState,
    private val updateState: ((SettingsState) -> SettingsState) -> Unit,
    private val onPageIntent: (PageIntent) -> Unit,
) {
    fun start() {
        // The choice is the repository's: the platform roots follow the same flows.
        tasks.launchMain {
            settings.themeMode.collect { mode -> updateState { it.copy(themes = SettingsOptions.themes(mode)) } }
        }
        tasks.launchMain {
            settings.language.collect { language ->
                updateState { it.copy(languages = SettingsOptions.languages(language)) }
            }
        }
        refreshCache()
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.SetTheme -> tasks.launchMain { settings.setThemeMode(intent.mode) }
            is SettingsIntent.SetLanguage -> tasks.launchMain { settings.setLanguage(intent.language) }
            SettingsIntent.ClearCache -> updateState { it.copy(confirmClear = true) }
            SettingsIntent.CancelClearCache -> updateState { it.copy(confirmClear = false) }
            SettingsIntent.ConfirmClearCache -> clearCache()
        }
    }

    /** The paginators fill the cache while the user browses, so the numbers are read again on demand. */
    fun refreshCache() {
        tasks.launchMain {
            val stats = cache.stats()
            updateState {
                it.copy(
                    cacheLists = UiText.Plain(stats.lists.toString()),
                    cacheSize = UiText.FileSize(stats.bytes),
                )
            }
        }
    }

    private fun clearCache() {
        updateState { it.copy(confirmClear = false) }
        tasks.launchMain {
            cache.clear()
            refreshCache()
        }
    }
}
