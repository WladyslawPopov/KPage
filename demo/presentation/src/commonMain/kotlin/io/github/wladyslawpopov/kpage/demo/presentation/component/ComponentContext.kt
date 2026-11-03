package io.github.wladyslawpopov.kpage.demo.presentation.component

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

/**
 * Lifecycle of one screen component. It lives as long as the screen is in the navigation and is
 * moved by the platform: RESUMED while shown, CREATED while hidden, DESTROYED when the screen leaves
 * the navigation. Main thread only.
 */
class ComponentContext : LifecycleOwner {
    private val registry =
        LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.CREATED }

    override val lifecycle: Lifecycle get() = registry

    /** Moves between CREATED, STARTED and RESUMED; ignored after [destroy]. */
    fun moveTo(state: Lifecycle.State) {
        require(state != Lifecycle.State.DESTROYED && state != Lifecycle.State.INITIALIZED) {
            "Use destroy() to finish the component"
        }
        if (registry.currentState == Lifecycle.State.DESTROYED) return
        registry.currentState = state
    }

    /** The screen is shown to the user. */
    fun show() = moveTo(Lifecycle.State.RESUMED)

    /** The screen is hidden: below in its stack, in another tab or the app is in the background. */
    fun hide() = moveTo(Lifecycle.State.CREATED)

    fun destroy() {
        if (registry.currentState == Lifecycle.State.DESTROYED) return
        registry.currentState = Lifecycle.State.DESTROYED
    }
}
