package io.github.wladyslawpopov.kpage.demo.presentation.screens.lab

/** UI of the Lab screen; every platform implements it and draws the state it gets. */
interface LabUi {
    fun renderLab(state: LabState)
}
