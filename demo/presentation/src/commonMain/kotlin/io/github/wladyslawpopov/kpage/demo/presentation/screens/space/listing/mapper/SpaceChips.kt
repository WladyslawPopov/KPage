package io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.mapper

import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceQuery
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.display.SpaceChip
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.display.SpaceChipTarget
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.options.SpaceFilterOptions
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.SpaceIntent
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.text.UiText
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey

/**
 * The chips of what is set: the search text, the center (by its short code: the full name is too long
 * for a chip), the years (one chip), the tag.
 */
internal fun spaceChips(query: SpaceQuery, searchText: String): List<SpaceChip> = buildList {
    val text = searchText.trim()
    if (text.isNotEmpty()) {
        add(SpaceChip(AppIconKey.SEARCH, UiText.Plain(text), SpaceChipTarget.SEARCH, SpaceIntent.SearchChanged("")))
    }
    query.center?.let { center ->
        add(
            SpaceChip(
                icon = null,
                text = UiText.Plain(center.name),
                opens = SpaceChipTarget.FILTERS,
                remove = SpaceIntent.SetFilters(null, query.yearStart, query.yearEnd, query.tag),
            )
        )
    }
    years(query.yearStart, query.yearEnd)?.let { years ->
        add(
            SpaceChip(
                icon = null,
                text = years,
                opens = SpaceChipTarget.FILTERS,
                remove = SpaceIntent.SetFilters(query.center, null, null, query.tag),
            )
        )
    }
    query.tag?.let { tag ->
        add(
            SpaceChip(
                icon = SpaceFilterOptions.icon(tag.kind),
                text = UiText.Plain(tag.value),
                opens = SpaceChipTarget.FILTERS,
                remove = SpaceIntent.SetFilters(query.center, query.yearStart, query.yearEnd, null),
            )
        )
    }
}

/** "1960–1969", "1969" for one year, or "From 1960" / "To 1969" when only one end is set. */
private fun years(from: Int?, to: Int?): UiText? = when {
    from != null && from == to -> UiText.Plain("$from")
    from != null && to != null -> UiText.Plain("$from–$to")
    from != null -> UiText.KeyWith(AppStringKey.Space.FROM, "$from")
    to != null -> UiText.KeyWith(AppStringKey.Space.TO, "$to")
    else -> null
}
