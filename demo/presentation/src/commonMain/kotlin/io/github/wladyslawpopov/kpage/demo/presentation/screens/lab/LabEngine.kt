package io.github.wladyslawpopov.kpage.demo.presentation.screens.lab

import io.github.wladyslawpopov.kpage.demo.domain.lab.LabRepository
import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentTasks
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageIntent

/** Business scenarios of the Lab tab. */
internal class LabEngine(
    private val tasks: ComponentTasks,
    private val repository: LabRepository,
    private val getState: () -> LabState,
    private val updateState: ((LabState) -> LabState) -> Unit,
    private val onPageIntent: (PageIntent) -> Unit,
) {
    fun start() = Unit

    fun onIntent(intent: LabIntent) = Unit
}
