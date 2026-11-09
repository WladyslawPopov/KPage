package io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.display

import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.SpaceIntent
import io.github.wladyslawpopov.kpage.demo.uicore.text.UiText
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey

/** What a tap on a chip opens: the search field or the filters; both are the state of the screen only. */
enum class SpaceChipTarget { SEARCH, FILTERS }

/**
 * A chip under the header: what is set. The UI draws it, opens [opens] on a tap and sends [remove] on its cross.
 * The UI decides only whether to show the [SpaceChipTarget.SEARCH] chip: not while the search field is on screen.
 */
data class SpaceChip(
    val icon: AppIconKey?,
    val text: UiText,
    val opens: SpaceChipTarget,
    val remove: SpaceIntent,
)
