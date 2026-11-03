package io.github.wladyslawpopov.kpage.demo.presentation.screens.settings

/** UI of the Settings screen; every platform implements it and draws the state it gets. */
interface SettingsUi {
    fun renderSettings(state: SettingsState)
}
