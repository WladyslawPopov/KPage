package io.github.wladyslawpopov.kpage.demo.presentation.page

import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey

/** What every page understands: from its UI (retry, close a toast) and from its engines. */
sealed interface PageIntent {
    data class SetLoading(val isLoading: Boolean) : PageIntent
    data class ShowToast(val text: AppStringKey, val type: ToastType) : PageIntent
    data object DismissToast : PageIntent
    data class OnError(val error: Throwable) : PageIntent
    data object ClearError : PageIntent

    /** Load the content again (retry, pull-to-refresh). */
    data object Refresh : PageIntent

    /** Return to the initial content (for example, drop the filters). */
    data object Reset : PageIntent

    /** The screen's own action of its empty state (for example, create the first item). */
    data object EmptyAction : PageIntent
}

interface PageOwner {
    fun onPageIntent(intent: PageIntent)
}
