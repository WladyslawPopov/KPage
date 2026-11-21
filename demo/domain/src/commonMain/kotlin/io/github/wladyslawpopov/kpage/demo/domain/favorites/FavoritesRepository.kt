package io.github.wladyslawpopov.kpage.demo.domain.favorites

import io.github.wladyslawpopov.kpage.core.paging.data.paginator.Paginator
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceImage
import kotlinx.coroutines.CoroutineScope

/** The images the user marked as favorite. */
interface FavoritesRepository {
    /** The favorites, newest first; the paginator lives as long as [scope]. */
    fun favorites(scope: CoroutineScope): Paginator<SpaceImage>

    /**
     * Saves the mark. Every list that shows the image gets the new mark at once, and the open
     * favorites list renews itself.
     */
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    /** Removes the marks of [ids] at once, as [setFavorite] does for one. */
    suspend fun removeFavorites(ids: Collection<String>)
}
