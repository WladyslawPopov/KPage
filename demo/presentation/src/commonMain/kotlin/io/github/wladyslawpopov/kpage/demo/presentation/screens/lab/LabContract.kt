package io.github.wladyslawpopov.kpage.demo.presentation.screens.lab

import io.github.wladyslawpopov.kpage.demo.presentation.page.PageEmptyState
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageScreenState
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageState
import io.github.wladyslawpopov.kpage.demo.uicore.model.TopBarState
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey

/** The Lab has no content yet: its page shows "coming soon" as the empty state, with nothing to refresh. */
data class LabState(
    override val page: PageState = PageState(
        empty = PageEmptyState(
            title = AppStringKey.Placeholder.COMING_SOON,
            icon = AppIconKey.LAB,
            actionText = null,
            action = null,
        ),
        isRefreshEnabled = false,
    ),
    val topBar: TopBarState = TopBarState(AppStringKey.Tabs.LAB),
) : PageScreenState<LabState> {
    override fun withPage(page: PageState) = copy(page = page)
}

sealed interface LabIntent
