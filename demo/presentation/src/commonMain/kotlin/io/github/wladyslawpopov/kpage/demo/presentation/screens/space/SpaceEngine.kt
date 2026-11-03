package io.github.wladyslawpopov.kpage.demo.presentation.screens.space

import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceRepository
import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentTasks
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageIntent

/** Business scenarios of the Space tab. */
internal class SpaceEngine(
    private val tasks: ComponentTasks,
    private val repository: SpaceRepository,
    private val getState: () -> SpaceState,
    private val updateState: ((SpaceState) -> SpaceState) -> Unit,
    private val onPageIntent: (PageIntent) -> Unit,
) {
    fun start() = Unit

    fun onIntent(intent: SpaceIntent) = Unit
}
