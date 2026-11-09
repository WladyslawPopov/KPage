package io.github.wladyslawpopov.kpage.demo.data.space

import io.github.wladyslawpopov.kpage.cache.PagingDatabase
import io.github.wladyslawpopov.kpage.core.paging.StablePaginator
import io.github.wladyslawpopov.kpage.core.paging.data.paginator.PagerPayload
import io.github.wladyslawpopov.kpage.core.paging.data.paginator.Paginator
import io.github.wladyslawpopov.kpage.core.paging.data.cache.updateItem
import io.github.wladyslawpopov.kpage.core.paging.data.paginator.PaginatorConfig
import io.github.wladyslawpopov.kpage.demo.data.favorites.FavoritesStore
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceImage
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceImageDetails
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceQuery
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceRepository
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceTagKind
import kotlinx.coroutines.CoroutineScope

internal class SpaceRepositoryImpl(
    private val remote: NasaImagesRemote,
    private val favorites: FavoritesStore,
    private val db: PagingDatabase,
) : SpaceRepository {

    override fun images(query: SpaceQuery, scope: CoroutineScope): Paginator<SpaceImage> =
        StablePaginator(
            serializer = SpaceImage.serializer(),
            queryKey = query.cacheKey(),
            // NASA counts pages from 1.
            config = PaginatorConfig(pageSize = PAGE_SIZE, initialPageKey = 1),
            getPage = { page -> loadPage(query, page) },
            idExtractor = { it.id },
            db = db,
            scope = scope,
        )

    override suspend fun details(id: String): SpaceImageDetails? {
        val item = remote.item(id) ?: return null
        val isFavorite = item.nasaId?.let { it in favorites.favoriteIds(listOf(it)) } ?: false
        return item.toSpaceImageDetails(isFavorite)
    }

    override suspend fun updateImage(id: String, transform: (SpaceImage) -> SpaceImage) {
        db.updateItem(SpaceImage.serializer(), id, transform = transform)
    }

    private suspend fun loadPage(query: SpaceQuery, page: Int): PagerPayload<SpaceImage> {
        val tag = query.tag
        fun tagValue(kind: SpaceTagKind) = tag?.value?.takeIf { tag.kind == kind }
        val collection = remote.search(
            NasaSearchRequest(
                text = query.text,
                center = query.center?.name,
                yearStart = query.yearStart,
                yearEnd = query.yearEnd,
                keywords = tagValue(SpaceTagKind.KEYWORD),
                photographer = tagValue(SpaceTagKind.PHOTOGRAPHER),
                secondaryCreator = tagValue(SpaceTagKind.CREATOR),
                location = tagValue(SpaceTagKind.LOCATION),
                album = tagValue(SpaceTagKind.ALBUM),
                page = page,
                pageSize = PAGE_SIZE,
            )
        ).collection
        // NASA answers 400 for anything past its first MAX_RESULTS results, so the list ends there.
        val totalCount = minOf(collection.metadata.totalHits, MAX_RESULTS)
        // NASA knows nothing about favorites: the mark comes from our database, asked once per page.
        val favoriteIds = favorites.favoriteIds(collection.items.mapNotNull { it.nasaId })
        return PagerPayload(
            totalCount = totalCount,
            isMore = page * PAGE_SIZE < totalCount,
            objects = collection.items.mapNotNull { it.toSpaceImage(favoriteIds) },
        )
    }

    private fun SpaceQuery.cacheKey() =
        "space|q=${text.trim()}|center=${center?.name.orEmpty()}|from=${yearStart ?: ""}|to=${yearEnd ?: ""}" +
            tag?.let { "|${it.kind.name.lowercase()}=${it.value}" }.orEmpty()

    private companion object {
        /** Divides [MAX_RESULTS], so the last allowed page is full and never crosses the limit. */
        const val PAGE_SIZE = 50
        const val MAX_RESULTS = 10_000
    }
}
