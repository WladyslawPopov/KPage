package io.github.wladyslawpopov.kpage.core.paging.data.cache

import io.github.wladyslawpopov.kpage.cache.PagingDatabase
import io.github.wladyslawpopov.kpage.core.paging.common.dbDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer

/**
 * The cache of one database, without a paginator: single items, stale lists and the cache as a
 * whole. A paginator owns its list; this owns everything else, so the data layer (a repository that
 * made a change on the server) keeps the cache up to date and the screens only show the lists.
 */
interface PagingStore {
    /** The cached items of one type; they are shared by every list of this type. */
    fun <T : Any> items(serializer: KSerializer<T>): ItemCache<T>

    /**
     * Marks the lists of [groups] stale: their live paginators (see the `group` of `StablePaginator`)
     * refresh where the user is, as after `refresh()`. A list that is not open refreshes when it is
     * created, as always. Call it after the server confirmed a change that adds, removes or moves items.
     */
    fun invalidate(vararg groups: String)

    /** What the cache holds, for example for a settings screen. */
    suspend fun stats(): PagingCacheStats

    /**
     * Clears the whole cache. With [refreshLive] the open lists refresh at once. On logout close the
     * lists first and pass `false`: no list goes to the server with the old session.
     */
    suspend fun clear(refreshLive: Boolean = true)

    /**
     * Sets how many lists the cache keeps; the setting is stored in the database. Opening a list beyond
     * it evicts the least recently used ones. Lists already beyond a lowered limit are evicted at once.
     */
    suspend fun setMaxLists(count: Int)

    /** The current limit of lists, see [setMaxLists]. */
    suspend fun maxLists(): Int
}

/** The cached items of one type. */
interface ItemCache<T : Any> {
    /** Reads the item from the cache; null if it is not cached or cannot be read. */
    suspend fun get(id: String): T?

    /**
     * Changes the cached item with [transform] in one step (read and write together), at every position
     * where it occurs, in every list of this type, the open ones too. A page load that started before
     * the change does not bring the old version back. Call it after the server confirmed the change.
     * Returns the new item, or null if the item is not cached or cannot be read (its JSON is kept).
     * An exception of [transform] is thrown to the caller.
     */
    suspend fun update(id: String, transform: (T) -> T): T?
}

/**
 * The store of [db]. [dispatcher] runs the database work: pass the `databaseDispatcher` of the
 * paginators if they were given their own.
 */
fun PagingStore(db: PagingDatabase, dispatcher: CoroutineDispatcher = dbDispatcher()): PagingStore =
    DatabasePagingStore(db, dispatcher)

private class DatabasePagingStore(
    private val db: PagingDatabase,
    private val dispatcher: CoroutineDispatcher,
) : PagingStore {

    override fun <T : Any> items(serializer: KSerializer<T>): ItemCache<T> =
        DatabaseItemCache(db, serializer, dispatcher)

    override fun invalidate(vararg groups: String) {
        if (groups.isNotEmpty()) LivePaginators.refresh(db, groups.toSet())
    }

    override suspend fun stats(): PagingCacheStats = db.cacheStats(dispatcher)

    override suspend fun clear(refreshLive: Boolean) {
        db.clearCache(dispatcher)
        if (refreshLive) LivePaginators.refresh(db, groups = null)
    }

    override suspend fun setMaxLists(count: Int) = db.setMaxLists(count, dispatcher)

    override suspend fun maxLists(): Int = db.maxLists(dispatcher)
}

private class DatabaseItemCache<T : Any>(
    private val db: PagingDatabase,
    private val serializer: KSerializer<T>,
    private val dispatcher: CoroutineDispatcher,
) : ItemCache<T> {

    // Items of different types may share an id, so the serial name is part of every key.
    private val itemType = serializer.descriptor.serialName

    override suspend fun get(id: String): T? = withContext(dispatcher) {
        db.itemRawQueries.selectById(itemType, id).executeAsOneOrNull()?.let { decode(it.json) }
    }

    override suspend fun update(id: String, transform: (T) -> T): T? = withContext(dispatcher) {
        db.transactionWithResult {
            val current = db.itemRawQueries.selectById(itemType, id).executeAsOneOrNull()?.let { decode(it.json) }
            current?.let(transform)?.also { next ->
                db.itemRawQueries.updateJson(CacheJson.encodeToString(serializer, next), itemType, id)
                ItemEdits.markEdited(db, itemType, id)
            }
        }
    }

    private fun decode(json: String): T? = try {
        CacheJson.decodeFromString(serializer, json)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        null
    }
}
