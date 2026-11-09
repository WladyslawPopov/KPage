package io.github.wladyslawpopov.kpage.demo.presentation.screens.space.details.mapper

import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceCenter
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceImageDetails
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceTag
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceTagKind
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.details.display.DetailsChip
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.details.display.DetailsPicture
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.details.display.DetailsRow
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.details.display.DetailsSection
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.details.display.SpaceDetailsDisplay
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.details.SpaceDetailsIntent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.options.SpaceFilterOptions
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.text.UiText
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey

/** The cards of the details screen: which there are, what they show and what a tap on a row or a chip does. */
internal fun SpaceImageDetails.toDisplay(): SpaceDetailsDisplay {
    val centerFilter = center?.let { code -> SpaceCenter.entries.firstOrNull { it.name.equals(code, ignoreCase = true) } }
    val year = date?.take(YEAR_LENGTH)?.toIntOrNull()

    val info = buildList {
        date?.let { add(DetailsRow(AppIconKey.DATE, AppStringKey.Space.DATE, UiText.Plain(it), year?.let(SpaceDetailsIntent::OpenYear))) }
        center?.let { code ->
            // A center the filter does not know is shown by its code and is not tappable.
            val value = centerFilter?.let { UiText.Key(SpaceFilterOptions.title(it)) } ?: UiText.Plain(code)
            add(DetailsRow(AppIconKey.CENTER, AppStringKey.Space.CENTER, value, centerFilter?.let(SpaceDetailsIntent::OpenCenter)))
        }
        addTagRow(SpaceTagKind.CREATOR, AppStringKey.Space.AUTHOR, creator)
        addTagRow(SpaceTagKind.PHOTOGRAPHER, AppStringKey.Space.PHOTOGRAPHER, photographer)
        addTagRow(SpaceTagKind.LOCATION, AppStringKey.Space.LOCATION, location)
    }

    // What the original file is: the picture above is a lighter copy of it.
    val originalWidth = original?.width
    val originalHeight = original?.height
    val file = buildList {
        originalFormat?.let { add(DetailsRow(null, AppStringKey.Space.FORMAT, UiText.Plain(it))) }
        if (originalWidth != null && originalHeight != null) {
            add(DetailsRow(null, AppStringKey.Space.RESOLUTION, UiText.Plain("$originalWidth × $originalHeight")))
        }
        original?.sizeBytes?.let { add(DetailsRow(null, AppStringKey.Space.FILE_SIZE, UiText.FileSize(it))) }
        add(DetailsRow(null, AppStringKey.Space.NASA_ID, UiText.Plain(id)))
    }

    return SpaceDetailsDisplay(
        title = title,
        description = description?.takeIf { it != title },
        picture = DetailsPicture(picture?.url, aspectRatio(picture?.width, picture?.height)),
        fullPictureUrl = fullPicture?.url,
        isFavorite = isFavorite,
        sections = buildList {
            if (info.isNotEmpty()) add(DetailsSection.Fields(AppStringKey.Space.DETAILS, info))
            if (keywords.isNotEmpty()) add(DetailsSection.Chips(AppStringKey.Space.KEYWORDS, tagChips(SpaceTagKind.KEYWORD, keywords)))
            if (albums.isNotEmpty()) add(DetailsSection.Chips(AppStringKey.Space.ALBUMS, tagChips(SpaceTagKind.ALBUM, albums)))
            add(DetailsSection.Fields(AppStringKey.Space.IMAGE, file))
        },
    )
}

private fun MutableList<DetailsRow>.addTagRow(kind: SpaceTagKind, label: AppStringKey, value: String?) {
    value ?: return
    add(DetailsRow(SpaceFilterOptions.icon(kind), label, UiText.Plain(value), SpaceDetailsIntent.OpenTag(SpaceTag(kind, value))))
}

private fun tagChips(kind: SpaceTagKind, values: List<String>) =
    values.map { DetailsChip(SpaceFilterOptions.icon(kind), it, SpaceDetailsIntent.OpenTag(SpaceTag(kind, it))) }

/** Width to height of the picture, kept between a portrait and a wide frame; 4:3 when NASA gave no sizes. */
private fun aspectRatio(width: Int?, height: Int?): Float {
    if (width == null || height == null || width <= 0 || height <= 0) return DEFAULT_RATIO
    return (width.toFloat() / height).coerceIn(MIN_RATIO, MAX_RATIO)
}

private const val YEAR_LENGTH = 4
private const val DEFAULT_RATIO = 4f / 3
private const val MIN_RATIO = 4f / 5
private const val MAX_RATIO = 2f
