package io.github.wladyslawpopov.kpage.demo.presentation.component

import kotlinx.coroutines.flow.StateFlow

/** MVI contract of a screen: one state for the UI, one entry for everything the user does. */
interface StateOwner<State : Any, Intent : Any> {
    val state: StateFlow<State>

    fun onIntent(intent: Intent)
}
