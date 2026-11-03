package io.github.wladyslawpopov.kpage.demo.presentation.screens.space

/** UI of the Space screen; every platform implements it and draws the state it gets. */
interface SpaceUi {
    fun renderSpace(state: SpaceState)
}
