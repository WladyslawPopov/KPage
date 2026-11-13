package io.github.wladyslawpopov.kpage.demo.presentation.page

import io.github.wladyslawpopov.kpage.demo.presentation.component.ScreenError
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey

enum class ToastType { SUCCESS, WARNING, FAIL, INFO }

/** [id] tells two equal messages apart, so the UI shows the second one too. */
data class PageToast(val id: Long, val text: AppStringKey, val type: ToastType)

/** Shown instead of an empty list; [action] is sent as a page intent from its button. */
data class PageEmptyState(
    val title: AppStringKey = AppStringKey.Core.NOTHING_FOUND,
    val icon: AppIconKey = AppIconKey.NOTHING_FOUND,
    val actionText: AppStringKey? = AppStringKey.Core.REFRESH,
    val action: PageIntent? = PageIntent.Refresh,
)

/**
 * What every page shows besides its content. [isRefreshEnabled] is only the pull-to-refresh gesture: the retry
 * of the error banner works on every page. [isLoading] is the loader: the first load or a refresh of the
 * shown data.
 */
data class PageState(
    val error: ScreenError? = null,
    val isLoading: Boolean = false,
    val toast: PageToast? = null,
    val empty: PageEmptyState? = null,
    val isRefreshEnabled: Boolean = true,
)

/** The state of a screen that is a page: it carries [page] and can return a copy with another one. */
interface PageScreenState<S : PageScreenState<S>> {
    val page: PageState

    fun withPage(page: PageState): S
}
