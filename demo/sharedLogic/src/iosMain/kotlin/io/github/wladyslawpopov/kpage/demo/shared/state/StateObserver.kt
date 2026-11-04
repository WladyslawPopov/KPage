package io.github.wladyslawpopov.kpage.demo.shared.state

import androidx.lifecycle.coroutineScope
import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** A UI subscribed to a component; [unbind] ends the subscription. */
fun interface UiBinding {
    fun unbind()
}

/**
 * Delivers the state of a component to Swift on Main. Stops on [UiBinding.unbind] or when the
 * component is destroyed, whichever comes first.
 */
class StateObserver<S : Any>(
    private val state: StateFlow<S>,
    private val context: ComponentContext,
) {
    fun observe(onChange: (S) -> Unit): UiBinding {
        // The lifecycle scope is cancelled on DESTROYED: a forgotten unbind cannot outlive the component.
        val job = context.lifecycle.coroutineScope.launch { state.collect(onChange) }
        return UiBinding { job.cancel() }
    }
}
