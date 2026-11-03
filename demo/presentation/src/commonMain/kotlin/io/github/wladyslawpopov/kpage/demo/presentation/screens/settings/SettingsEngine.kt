package io.github.wladyslawpopov.kpage.demo.presentation.screens.settings

import io.github.wladyslawpopov.kpage.demo.domain.settings.SettingsRepository
import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentTasks
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageIntent

/** Business scenarios of the Settings tab. */
internal class SettingsEngine(
    private val tasks: ComponentTasks,
    private val repository: SettingsRepository,
    private val getState: () -> SettingsState,
    private val updateState: ((SettingsState) -> SettingsState) -> Unit,
    private val onPageIntent: (PageIntent) -> Unit,
) {
    fun start() = Unit

    fun onIntent(intent: SettingsIntent) = Unit
}
