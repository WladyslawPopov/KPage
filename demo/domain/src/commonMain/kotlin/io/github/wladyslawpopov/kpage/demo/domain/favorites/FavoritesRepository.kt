package io.github.wladyslawpopov.kpage.demo.domain.favorites

import io.github.wladyslawpopov.kpage.core.paging.data.paginator.Paginator
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

/** The images the user marked as favorite. */
interface FavoritesRepository {
    /** The favorites, newest first; the paginator lives as long as [scope]. */
    fun favorites(scope: CoroutineScope): Paginator<SpaceImage>

    /**
     * Emits after every change of the favorites, whoever made it, so the favorites list knows when to
     * `refresh()`. Changes that come close together may be reported once.
     */
    val changes: Flow<Unit>

    /**
     * Saves the mark. A list that shows the image changes it with `updateItem`; the favorites list
     * renews on [changes].
     */
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    /** Removes the marks of [ids] at once. */
    suspend fun removeFavorites(ids: Collection<String>)
}
