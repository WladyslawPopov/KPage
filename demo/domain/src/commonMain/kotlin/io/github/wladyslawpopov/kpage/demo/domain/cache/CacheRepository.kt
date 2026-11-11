package io.github.wladyslawpopov.kpage.demo.domain.cache

import kotlinx.coroutines.flow.Flow

/** What the paginators keep on the device: the number of lists and the size of their data. */
data class CacheStats(val lists: Int, val bytes: Long)

/** The cache of the paginators. */
interface CacheRepository {
    /**
     * Emits after the cache was cleared. The open lists lose their pages with it and stay empty until
     * they are refreshed, so the screens that own a list renew it on this signal.
     */
    val cleared: Flow<Unit>

    suspend fun stats(): CacheStats

    suspend fun clear()
}
