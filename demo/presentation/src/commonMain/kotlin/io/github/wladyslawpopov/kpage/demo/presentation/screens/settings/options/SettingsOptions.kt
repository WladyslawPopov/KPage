package io.github.wladyslawpopov.kpage.demo.presentation.screens.settings.options

import io.github.wladyslawpopov.kpage.demo.domain.settings.AppLanguage
import io.github.wladyslawpopov.kpage.demo.domain.settings.ThemeMode
import io.github.wladyslawpopov.kpage.demo.presentation.screens.settings.display.SettingsOption
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.text.UiText

/** The choices of the settings with the current one marked. */
internal object SettingsOptions {

    fun themes(selected: ThemeMode): List<SettingsOption<ThemeMode>> =
        ThemeMode.entries.map { SettingsOption(it, UiText.Key(it.label()), it == selected) }

    // A language is named in itself, in every interface language.
    fun languages(selected: AppLanguage): List<SettingsOption<AppLanguage>> =
        AppLanguage.entries.map { SettingsOption(it, UiText.Plain(it.nativeName()), it == selected) }

    private fun ThemeMode.label() = when (this) {
        ThemeMode.SYSTEM -> AppStringKey.Settings.THEME_SYSTEM
        ThemeMode.LIGHT -> AppStringKey.Settings.THEME_LIGHT
        ThemeMode.DARK -> AppStringKey.Settings.THEME_DARK
    }

    private fun AppLanguage.nativeName() = when (this) {
        AppLanguage.ENGLISH -> "English"
        AppLanguage.POLISH -> "Polski"
        AppLanguage.UKRAINIAN -> "Українська"
        AppLanguage.RUSSIAN -> "Русский"
    }
}
