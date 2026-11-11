package io.github.wladyslawpopov.kpage.demo.presentation.screens.settings

import io.github.wladyslawpopov.kpage.demo.domain.settings.AppLanguage
import io.github.wladyslawpopov.kpage.demo.domain.settings.ThemeMode
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageScreenState
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageState
import io.github.wladyslawpopov.kpage.demo.presentation.screens.settings.display.SettingsOption
import io.github.wladyslawpopov.kpage.demo.presentation.screens.settings.options.SettingsOptions
import io.github.wladyslawpopov.kpage.demo.uicore.model.TopBarState
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.text.UiText

/**
 * @property cacheLists The number of lists the paginators keep, ready to show.
 * @property cacheSize The size of their data.
 * @property confirmClear The cache is about to be cleared: the screen asks the user first.
 */
data class SettingsState(
    override val page: PageState = PageState(),
    val topBar: TopBarState = TopBarState(AppStringKey.Tabs.SETTINGS),
    val themes: List<SettingsOption<ThemeMode>> = SettingsOptions.themes(ThemeMode.SYSTEM),
    val languages: List<SettingsOption<AppLanguage>> = SettingsOptions.languages(AppLanguage.ENGLISH),
    val cacheLists: UiText = UiText.Plain("0"),
    val cacheSize: UiText = UiText.FileSize(0),
    val confirmClear: Boolean = false,
) : PageScreenState<SettingsState> {
    override fun withPage(page: PageState) = copy(page = page)
}

sealed interface SettingsIntent {
    data class SetTheme(val mode: ThemeMode) : SettingsIntent

    data class SetLanguage(val language: AppLanguage) : SettingsIntent

    data object ClearCache : SettingsIntent

    data object ConfirmClearCache : SettingsIntent

    data object CancelClearCache : SettingsIntent
}
