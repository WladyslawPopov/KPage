package io.github.wladyslawpopov.kpage.core.paging.data.cache

import io.github.wladyslawpopov.kpage.cache.PagingDatabase
import io.github.wladyslawpopov.kpage.core.paging.common.dbDispatcher
import io.github.wladyslawpopov.kpage.core.paging.data.DEFAULT_MAX_LISTS
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * What the cache holds, for example for a settings screen.
 *
 * @property lists Lists (`queryKey`s) kept in the cache.
 * @property items Items kept in the cache; an item shared by several lists counts once.
 * @property dataBytes Size of the stored items in bytes. It drops right after a cleanup, unlike the
 * database file, which SQLite does not shrink.
 */
data class PagingCacheStats(val lists: Int, val items: Int, val dataBytes: Long)

// Every function below takes the database dispatcher of the paginators: pass the same one if the
// paginators were given their own `databaseDispatcher`.

/** Clears persisted pages after their owning paginators have been closed. */
suspend fun PagingDatabase.clearCache(dispatcher: CoroutineDispatcher = dbDispatcher()) {
    withContext(dispatcher) {
        transaction {
            listingEntryQueries.clearAll()
            pageMetadataQueries.clearAll()
            queryMetaQueries.clearAll()
            itemRawQueries.clearAll()
        }
    }
}

/** Clears one persisted list after its paginator has been closed; other lists are kept. */
suspend fun PagingDatabase.clearCache(queryKey: String, dispatcher: CoroutineDispatcher = dbDispatcher()) {
    withContext(dispatcher) {
        transaction {
            deleteQueryData(queryKey)
            itemRawQueries.deleteUnreferenced()
        }
    }
}

/**
 * Sets how many lists the cache keeps; the setting is stored in the database. Opening a list beyond
 * it evicts the least recently used ones. Lists already beyond a lowered limit are evicted at once.
 */
suspend fun PagingDatabase.setMaxLists(count: Int, dispatcher: CoroutineDispatcher = dbDispatcher()) {
    require(count >= 1) { "count must be at least 1" }
    withContext(dispatcher) {
        transaction {
            cacheSettingsQueries.setMaxLists(count.toLong())
            evictOldLists()
        }
    }
}

/** The current limit of lists, see [setMaxLists]. */
suspend fun PagingDatabase.maxLists(dispatcher: CoroutineDispatcher = dbDispatcher()): Int =
    withContext(dispatcher) { readMaxLists() }

/**
 * Changes the cached item with [transform] in one step, without a paginator: for example, a details
 * screen changed it on the server. The change reaches every list of this item type, the open ones
 * too. Unlike [Paginator.updateItem][io.github.wladyslawpopov.kpage.core.paging.data.paginator.Paginator.updateItem],
 * a load that started before the edit may bring the old version back until its list is refreshed.
 * Returns the new item, or null if the item is not cached.
 */
suspend fun <T : Any> PagingDatabase.updateItem(
    serializer: KSerializer<T>,
    id: String,
    dispatcher: CoroutineDispatcher = dbDispatcher(),
    transform: (T) -> T,
): T? = withContext(dispatcher) {
    val itemType = serializer.descriptor.serialName
    transactionWithResult {
        val raw = itemRawQueries.selectById(itemType, id).executeAsOneOrNull()
        val current = raw?.let { CacheJson.decodeFromString(serializer, it.json) }
        current?.let(transform)?.also { next ->
            itemRawQueries.updateJson(CacheJson.encodeToString(serializer, next), itemType, id)
        }
    }
}

suspend fun PagingDatabase.cacheStats(dispatcher: CoroutineDispatcher = dbDispatcher()): PagingCacheStats =
    withContext(dispatcher) {
    transactionWithResult {
        PagingCacheStats(
            lists = queryMetaQueries.countQueries().executeAsOne().toInt(),
            items = itemRawQueries.countItems().executeAsOne().toInt(),
            dataBytes = itemRawQueries.dataSize().executeAsOne(),
        )
    }
}

/** How items are stored in the cache; the paginators and the functions above share it. */
internal val CacheJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    coerceInputValues = true
}

internal fun PagingDatabase.readMaxLists(): Int =
    cacheSettingsQueries.getMaxLists().executeAsOneOrNull()?.toInt() ?: DEFAULT_MAX_LISTS

/** Called inside a transaction: deletes the least recently used lists beyond the limit. */
internal fun PagingDatabase.evictOldLists() {
    queryMetaQueries.getQueryKeysToEvict(readMaxLists().toLong()).executeAsList()
        .forEach { deleteQueryData(it) }
    itemRawQueries.deleteUnreferenced()
}

/**
 * Cache keys of the live paginators of every database. A second live paginator for the same
 * `queryKey` (the same list on two screens) gets a key of its own, so they do not delete each other's
 * pages. The first one keeps the plain key, so its cache survives restarts as usual.
 */
@OptIn(ExperimentalAtomicApi::class)
internal object LiveCacheKeys {
    private val live = AtomicReference(emptyMap<PagingDatabase, Set<String>>())

    fun acquire(db: PagingDatabase, queryKey: String): String {
        while (true) {
            val current = live.load()
            val keys = current[db].orEmpty()
            val key = generateSequence(1) { it + 1 }
                .map { if (it == 1) queryKey else "$queryKey#$it" }
                .first { it !in keys }
            if (live.compareAndSet(current, current + (db to keys + key))) return key
        }
    }

    fun release(db: PagingDatabase, key: String) {
        while (true) {
            val current = live.load()
            val keys = current[db].orEmpty() - key
            val next = if (keys.isEmpty()) current - db else current + (db to keys)
            if (live.compareAndSet(current, next)) return
        }
    }
}

/** Deletes everything stored for [queryKey]; items are left for [deleteUnreferenced][io.github.wladyslawpopov.kpage.cache.ItemRawQueries.deleteUnreferenced]. */
internal fun PagingDatabase.deleteQueryData(queryKey: String) {
    listingEntryQueries.deleteAllEntriesByQuery(queryKey)
    pageMetadataQueries.deleteAllPageMetadataByQuery(queryKey)
    queryMetaQueries.deleteByKey(queryKey)
}
