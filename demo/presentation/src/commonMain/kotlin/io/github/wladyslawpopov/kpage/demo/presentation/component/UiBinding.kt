package io.github.wladyslawpopov.kpage.demo.presentation.component

/** A UI subscribed to its component; [unbind] ends the subscription. */
fun interface UiBinding {
    fun unbind()
}
