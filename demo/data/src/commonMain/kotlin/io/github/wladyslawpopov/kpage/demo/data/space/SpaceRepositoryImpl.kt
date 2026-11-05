package io.github.wladyslawpopov.kpage.demo.data.space

import io.github.wladyslawpopov.kpage.cache.PagingDatabase
import io.github.wladyslawpopov.kpage.core.paging.StablePaginator
import io.github.wladyslawpopov.kpage.core.paging.data.paginator.PagerPayload
import io.github.wladyslawpopov.kpage.core.paging.data.paginator.Paginator
import io.github.wladyslawpopov.kpage.core.paging.data.paginator.PaginatorConfig
import io.github.wladyslawpopov.kpage.demo.data.favorites.FavoritesStore
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceImage
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceQuery
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceRepository
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

    private suspend fun loadPage(query: SpaceQuery, page: Int): PagerPayload<SpaceImage> {
        val collection = remote.search(
            NasaSearchRequest(
                text = query.text,
                center = query.center?.name,
                yearStart = query.yearStart,
                yearEnd = query.yearEnd,
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
        "space|q=${text.trim()}|center=${center?.name.orEmpty()}|from=${yearStart ?: ""}|to=${yearEnd ?: ""}"

    private companion object {
        /** Divides [MAX_RESULTS], so the last allowed page is full and never crosses the limit. */
        const val PAGE_SIZE = 50
        const val MAX_RESULTS = 10_000
    }
}
