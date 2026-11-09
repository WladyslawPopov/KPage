package io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing

import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceCenter
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceTag

sealed interface SpaceIntent {
    /** The text of the search field changed. */
    data class SearchChanged(val text: String) : SpaceIntent

    /** Sets all the filters at once; null clears a filter. The list changes at once. */
    data class SetFilters(
        val center: SpaceCenter?,
        val yearStart: Int?,
        val yearEnd: Int?,
        val tag: SpaceTag?,
    ) : SpaceIntent

    data object ToggleLayout : SpaceIntent

    /** Opens the details of an image. */
    data class OpenImage(val id: String) : SpaceIntent

    /** The heart of an image. The value is sent, not toggled, so a repeated tap cannot flip it twice. */
    data class SetFavorite(val id: String, val isFavorite: Boolean) : SpaceIntent
}
