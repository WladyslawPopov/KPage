package io.github.wladyslawpopov.kpage.demo.data.cache

import io.github.wladyslawpopov.kpage.cache.PagingDatabase
import io.github.wladyslawpopov.kpage.core.paging.data.cache.cacheStats
import io.github.wladyslawpopov.kpage.core.paging.data.cache.clearCache
import io.github.wladyslawpopov.kpage.demo.domain.cache.CacheRepository
import io.github.wladyslawpopov.kpage.demo.domain.cache.CacheStats
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

internal class CacheRepositoryImpl(private val db: PagingDatabase) : CacheRepository {

    private val clearedSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val cleared: Flow<Unit> = clearedSignal

    override suspend fun stats(): CacheStats =
        db.cacheStats().let { CacheStats(lists = it.lists, bytes = it.dataBytes) }

    override suspend fun clear() {
        db.clearCache()
        clearedSignal.tryEmit(Unit)
    }
}
