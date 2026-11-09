package io.github.wladyslawpopov.kpage.demo.presentation.screens.space.details

import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceCenter
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceTag

sealed interface SpaceDetailsIntent {
    /** The value is sent, not toggled, so a repeated tap cannot flip it twice. */
    data class SetFavorite(val isFavorite: Boolean) : SpaceDetailsIntent

    // The Space list with one filter taken from the image.
    data class OpenCenter(val center: SpaceCenter) : SpaceDetailsIntent
    data class OpenYear(val year: Int) : SpaceDetailsIntent
    data class OpenTag(val tag: SpaceTag) : SpaceDetailsIntent
}
