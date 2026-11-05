package io.github.wladyslawpopov.kpage.demo.domain.space

import kotlinx.serialization.Serializable

/**
 * An image in the Space list. Serializable because the paginator keeps it in its cache.
 *
 * @property previewUrl The small picture, or null when NASA has none.
 * @property date The day it was created, `yyyy-MM-dd`.
 * @property center The NASA center code as the server gave it.
 * @property isFavorite Whether the user marked it. NASA knows nothing about it: the repositories set it.
 */
@Serializable
data class SpaceImage(
    val id: String,
    val title: String,
    val previewUrl: String?,
    val date: String?,
    val center: String?,
    val isFavorite: Boolean = false,
)
