package io.github.wladyslawpopov.kpage.demo.domain.space

/**
 * Everything NASA tells about an image, for its details screen.
 *
 * @property date The day it was created, `yyyy-MM-dd`.
 * @property center The NASA center code as the server gave it.
 * @property creator Who made the image when it is not a photo, for example a mission team.
 * @property picture The picture to show: large enough for a screen, much lighter than [original].
 * @property fullPicture The picture to look at full screen and zoom: the largest copy NASA made, or [picture].
 * @property original The file as NASA keeps it.
 * @property originalFormat The format of [original] by its extension, for example `JPG` or `TIF`.
 */
data class SpaceImageDetails(
    val id: String,
    val title: String,
    val description: String?,
    val date: String?,
    val center: String?,
    val photographer: String?,
    val creator: String?,
    val location: String?,
    val keywords: List<String>,
    val albums: List<String>,
    val picture: SpaceImageFile?,
    val fullPicture: SpaceImageFile?,
    val original: SpaceImageFile?,
    val originalFormat: String?,
    val isFavorite: Boolean,
)

/** A file of an image; NASA gives the sizes not for every file. */
data class SpaceImageFile(
    val url: String,
    val width: Int?,
    val height: Int?,
    val sizeBytes: Long?,
)
