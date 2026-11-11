package io.github.wladyslawpopov.kpage.demo.presentation.screens.settings.display

import io.github.wladyslawpopov.kpage.demo.uicore.text.UiText

/** One choice of a setting, ready to draw; the screen sends [value] back as it is. */
data class SettingsOption<T : Any>(val value: T, val label: UiText, val selected: Boolean)
