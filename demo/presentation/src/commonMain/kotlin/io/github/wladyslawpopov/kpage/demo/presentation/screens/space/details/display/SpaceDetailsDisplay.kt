package io.github.wladyslawpopov.kpage.demo.presentation.screens.space.details.display

import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.details.SpaceDetailsIntent
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.text.UiText
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey

/**
 * Everything the details screen draws, ready: the UI reads it and builds nothing itself.
 *
 * @property description Null when there is none or it only repeats [title].
 * @property fullPictureUrl The picture for the full-screen view; null when there is none.
 */
data class SpaceDetailsDisplay(
    val title: String,
    val description: String?,
    val picture: DetailsPicture,
    val fullPictureUrl: String?,
    val isFavorite: Boolean,
    val sections: List<DetailsSection>,
)

/** The picture under the bar; [aspectRatio] (width to height) is already kept within sensible limits. */
data class DetailsPicture(val url: String?, val aspectRatio: Float)

/** A titled card of the screen. */
sealed interface DetailsSection {
    val title: AppStringKey

    data class Fields(override val title: AppStringKey, val rows: List<DetailsRow>) : DetailsSection

    data class Chips(override val title: AppStringKey, val chips: List<DetailsChip>) : DetailsSection
}

/** A field: its name over its value. With an [action] the row is tappable and the UI sends it as it is. */
data class DetailsRow(
    val icon: AppIconKey?,
    val label: AppStringKey,
    val value: UiText,
    val action: SpaceDetailsIntent? = null,
)

data class DetailsChip(val icon: AppIconKey, val text: String, val action: SpaceDetailsIntent)
