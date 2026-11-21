package io.github.wladyslawpopov.kpage.demo.data.cache

import io.github.wladyslawpopov.kpage.core.paging.data.cache.PagingStore
import io.github.wladyslawpopov.kpage.demo.domain.cache.CacheRepository
import io.github.wladyslawpopov.kpage.demo.domain.cache.CacheStats

internal class CacheRepositoryImpl(private val store: PagingStore) : CacheRepository {

    override suspend fun stats(): CacheStats =
        store.stats().let { CacheStats(lists = it.lists, bytes = it.dataBytes) }

    override suspend fun clear() = store.clear()
}
