package io.github.wladyslawpopov.kpage.demo.data.space

import io.github.wladyslawpopov.kpage.demo.data.space.dto.NasaItemDto
import io.github.wladyslawpopov.kpage.demo.data.space.dto.NasaLinkDto
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceImage
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceImageDetails
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceImageFile

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

/** Null for an item without data, as [toSpaceImage]. */
internal fun NasaItemDto.toSpaceImageDetails(isFavorite: Boolean): SpaceImageDetails? {
    val item = data.firstOrNull() ?: return null
    val original = links.firstOrNull { it.rel == "canonical" }
    // The original may be a huge TIF, so both pictures are copies NASA made of it.
    val picture = PICTURE_SIZES.firstNotNullOfOrNull { size -> links.firstOrNull { it.href.contains(size) } }?.toFile()
    val fullPicture = links.firstOrNull { it.href.contains(LARGE) }?.toFile() ?: picture
    return SpaceImageDetails(
        id = item.nasaId,
        title = item.title.orBlankNull() ?: item.nasaId,
        description = item.description.orBlankNull(),
        date = item.dateCreated?.take(DATE_LENGTH),
        center = item.center.orBlankNull(),
        photographer = item.photographer.orBlankNull(),
        creator = item.secondaryCreator.orBlankNull(),
        location = item.location.orBlankNull(),
        keywords = item.keywords.mapNotNull { it.orBlankNull() }.distinct(),
        albums = item.album.mapNotNull { it.orBlankNull() }.distinct(),
        picture = picture,
        fullPicture = fullPicture,
        original = original?.toFile(),
        originalFormat = original?.href?.substringAfterLast('/')?.substringAfterLast('.', "")
            ?.uppercase()?.ifEmpty { null },
        isFavorite = isFavorite,
    )
}

private fun NasaLinkDto.toFile() = SpaceImageFile(url = href, width = width, height = height, sizeBytes = size)

private fun String?.orBlankNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

private const val DATE_LENGTH = "yyyy-MM-dd".length

/** The copies of an image by their file names, the best fit for a phone screen first. */
private val PICTURE_SIZES = listOf("~medium.", "~large.", "~small.", "~thumb.")

private const val LARGE = "~large."
