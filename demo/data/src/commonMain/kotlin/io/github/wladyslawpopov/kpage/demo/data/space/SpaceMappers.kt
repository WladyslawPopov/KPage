package io.github.wladyslawpopov.kpage.demo.data.space

import io.github.wladyslawpopov.kpage.demo.data.space.dto.NasaItemDto
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceImage

/** The id of the item, or null for an item without data. */
internal val NasaItemDto.nasaId: String? get() = data.firstOrNull()?.nasaId

/**
 * Null for an item without data: it has no id, so nothing can be shown. [favoriteIds] are the ids the
 * user marked, known before mapping.
 */
internal fun NasaItemDto.toSpaceImage(favoriteIds: Set<String> = emptySet()): SpaceImage? {
    val item = data.firstOrNull() ?: return null
    return SpaceImage(
        id = item.nasaId,
        title = item.title?.takeIf { it.isNotBlank() } ?: item.nasaId,
        // Only the small picture: the only other link of an item may be the original file.
        previewUrl = links.firstOrNull { it.rel == "preview" }?.href,
        date = item.dateCreated?.take(DATE_LENGTH),
        center = item.center,
        isFavorite = item.nasaId in favoriteIds,
    )
}

private const val DATE_LENGTH = "yyyy-MM-dd".length
