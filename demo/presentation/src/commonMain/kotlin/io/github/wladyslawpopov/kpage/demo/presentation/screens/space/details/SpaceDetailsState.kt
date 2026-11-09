package io.github.wladyslawpopov.kpage.demo.presentation.screens.space.details

import io.github.wladyslawpopov.kpage.demo.presentation.page.PageScreenState
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageState
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.details.display.SpaceDetailsDisplay

/** @property display Null until the image is loaded, or when NASA has no such image (then the page is empty). */
data class SpaceDetailsState(
    override val page: PageState = PageState(isLoading = true),
    val display: SpaceDetailsDisplay? = null,
) : PageScreenState<SpaceDetailsState> {
    override fun withPage(page: PageState) = copy(page = page)
}
