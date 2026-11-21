package io.github.wladyslawpopov.kpage.demo.domain.cache

/** What the paginators keep on the device: the number of lists and the size of their data. */
data class CacheStats(val lists: Int, val bytes: Long)

/** The cache of the paginators. */
interface CacheRepository {
    suspend fun stats(): CacheStats

    /** Clears the cache; the open lists load their data again by themselves. */
    suspend fun clear()
}
