package io.github.wladyslawpopov.kpage.demo.presentation.screens.settings

import io.github.wladyslawpopov.kpage.demo.domain.settings.SettingsRepository
import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentContext
import io.github.wladyslawpopov.kpage.demo.presentation.component.StateOwner
import io.github.wladyslawpopov.kpage.demo.presentation.component.UiBinding
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.AppNavigator
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.ScreenComponent
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageComponent
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageOwner

interface SettingsComponent : ScreenComponent, StateOwner<SettingsState, SettingsIntent>, PageOwner {
    /** Subscribes [ui] to the state of this screen. */
    fun bindSettings(ui: SettingsUi): UiBinding
}

internal class DefaultSettingsComponent(
    context: ComponentContext,
    private val navigator: AppNavigator,
    repository: SettingsRepository,
) : PageComponent<SettingsState, SettingsIntent>(context, SettingsState()), SettingsComponent {

    private val engine = SettingsEngine(
        tasks = this,
        repository = repository,
        getState = { currentState },
        updateState = ::updateState,
        onPageIntent = ::onPageIntent,
    )

    init {
        engine.start()
    }

    override fun onIntent(intent: SettingsIntent) = engine.onIntent(intent)

    override fun bindSettings(ui: SettingsUi): UiBinding = bindState(ui::renderSettings)
}
