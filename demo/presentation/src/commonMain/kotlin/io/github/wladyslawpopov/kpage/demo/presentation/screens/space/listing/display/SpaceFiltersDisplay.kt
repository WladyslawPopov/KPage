package io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.display

import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.options.SpaceFilterOptions

/**
 * What the filters panel draws besides the options in [SpaceFilterOptions]. The years are listed
 * newest first; the range cannot end before it starts, so each list offers only the years that keep it valid.
 *
 * @property resultCount How many images the filters leave; null while the list loads.
 */
data class SpaceFiltersDisplay(
    val fromYears: List<Int>,
    val toYears: List<Int>,
    val resultCount: Int?,
)
