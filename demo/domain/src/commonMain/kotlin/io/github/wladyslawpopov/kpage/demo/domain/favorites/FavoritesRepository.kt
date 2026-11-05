package io.github.wladyslawpopov.kpage.demo.domain.favorites

import io.github.wladyslawpopov.kpage.core.paging.data.paginator.Paginator
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceImage
import kotlinx.coroutines.CoroutineScope

/** The images the user marked as favorite. */
interface FavoritesRepository {
    /** The favorites, newest first; the paginator lives as long as [scope]. */
    fun favorites(scope: CoroutineScope): Paginator<SpaceImage>

    /**
     * Saves the mark. A list that shows the image changes it with `updateItem`; the favorites list
     * renews with `refresh()`.
     */
    suspend fun setFavorite(id: String, isFavorite: Boolean)
}
