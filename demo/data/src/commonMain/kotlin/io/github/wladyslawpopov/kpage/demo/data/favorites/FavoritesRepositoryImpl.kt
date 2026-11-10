package io.github.wladyslawpopov.kpage.demo.data.favorites

import io.github.wladyslawpopov.kpage.cache.PagingDatabase
import io.github.wladyslawpopov.kpage.core.paging.StablePaginator
import io.github.wladyslawpopov.kpage.core.paging.data.paginator.PagerPayload
import io.github.wladyslawpopov.kpage.core.paging.data.paginator.Paginator
import io.github.wladyslawpopov.kpage.core.paging.data.paginator.PaginatorConfig
import io.github.wladyslawpopov.kpage.demo.data.space.NasaImagesRemote
import io.github.wladyslawpopov.kpage.demo.data.space.toSpaceImage
import io.github.wladyslawpopov.kpage.demo.domain.favorites.FavoritesRepository
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * NASA has no favorites, so this stands in for the server that would keep them: the ids are ours, and
 * the images behind them are fetched one request each into [fetched].
 *
 * A page is read only from [fetched] after [sync], so the pages of one refresh see the same ids and
 * the same total, however many run at once.
 */
internal class FavoritesRepositoryImpl(
    private val remote: NasaImagesRemote,
    private val store: FavoritesStore,
    private val db: PagingDatabase,
) : FavoritesRepository {

    // The images fetched since the app started: a refresh of the list asks NASA only for the new ones.
    private val fetched = MutableStateFlow(emptyMap<String, SpaceImage>())
    private val syncLock = Mutex()
    private val changed = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val changes: Flow<Unit> = changed

    override fun favorites(scope: CoroutineScope): Paginator<SpaceImage> =
        StablePaginator(
            serializer = SpaceImage.serializer(),
            queryKey = QUERY_KEY,
            config = PaginatorConfig(pageSize = PAGE_SIZE),
            getPage = ::loadPage,
            idExtractor = { it.id },
            db = db,
            scope = scope,
        )

    override suspend fun setFavorite(id: String, isFavorite: Boolean) {
        if (isFavorite) store.add(id) else store.remove(id)
        changed.tryEmit(Unit)
    }

    override suspend fun removeFavorites(ids: Collection<String>) {
        store.removeAll(ids)
        changed.tryEmit(Unit)
    }

    private suspend fun loadPage(page: Int): PagerPayload<SpaceImage> {
        sync()
        val ids = store.page(offset = page * PAGE_SIZE, limit = PAGE_SIZE)
        val totalCount = store.count()
        val images = fetched.value
        return PagerPayload(
            totalCount = totalCount,
            isMore = (page + 1) * PAGE_SIZE < totalCount,
            objects = ids.mapNotNull { images[it] },
        )
    }

    /**
     * Fetches the images of the ids that have none yet. Runs one at a time: a caller that waits for
     * the lock finds nothing left to do.
     */
    private suspend fun sync() = syncLock.withLock {
        val missing = store.allIds().filter { it !in fetched.value }
        if (missing.isEmpty()) return@withLock
        val results = coroutineScope { missing.map { id -> async { id to fetch(id) } }.awaitAll() }
        fetched.update { images -> images + results.mapNotNull { (id, r) -> r.getOrNull()?.let { id to it } } }
        // NASA answered and has no such image: it is gone for good, so the id goes silently.
        // A failed request says nothing about the image, so its id stays.
        store.removeAll(results.filter { (_, r) -> r.isSuccess && r.getOrNull() == null }.map { it.first })
        results.firstNotNullOfOrNull { (_, r) -> r.exceptionOrNull() }?.let { throw it }
    }

    private suspend fun fetch(id: String): Result<SpaceImage?> = try {
        Result.success(remote.item(id)?.toSpaceImage(setOf(id)))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    private companion object {
        const val QUERY_KEY = "favorites"
        // Every item is a request, so the page is small.
        const val PAGE_SIZE = 20
    }
}
