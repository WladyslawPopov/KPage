package io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.mapper

import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceQuery
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.display.SpaceFiltersDisplay
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.options.SpaceFilterOptions

internal fun spaceFilters(query: SpaceQuery, totalCount: Int, isLoading: Boolean): SpaceFiltersDisplay {
    val first = SpaceFilterOptions.firstYear
    val last = SpaceFilterOptions.lastYear
    return SpaceFiltersDisplay(
        fromYears = (first..(query.yearEnd ?: last)).reversed().toList(),
        toYears = ((query.yearStart ?: first)..last).reversed().toList(),
        resultCount = if (isLoading) null else totalCount,
    )
}
