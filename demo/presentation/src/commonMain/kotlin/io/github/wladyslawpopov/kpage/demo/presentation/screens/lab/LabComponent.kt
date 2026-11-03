package io.github.wladyslawpopov.kpage.demo.presentation.screens.lab

import io.github.wladyslawpopov.kpage.demo.domain.lab.LabRepository
import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentContext
import io.github.wladyslawpopov.kpage.demo.presentation.component.StateOwner
import io.github.wladyslawpopov.kpage.demo.presentation.component.UiBinding
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.AppNavigator
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.ScreenComponent
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageComponent
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageOwner

interface LabComponent : ScreenComponent, StateOwner<LabState, LabIntent>, PageOwner {
    /** Subscribes [ui] to the state of this screen. */
    fun bindLab(ui: LabUi): UiBinding
}

internal class DefaultLabComponent(
    context: ComponentContext,
    private val navigator: AppNavigator,
    repository: LabRepository,
) : PageComponent<LabState, LabIntent>(context, LabState()), LabComponent {

    private val engine = LabEngine(
        tasks = this,
        repository = repository,
        getState = { currentState },
        updateState = ::updateState,
        onPageIntent = ::onPageIntent,
    )

    init {
        engine.start()
    }

    override fun onIntent(intent: LabIntent) = engine.onIntent(intent)

    override fun bindLab(ui: LabUi): UiBinding = bindState(ui::renderLab)
}
