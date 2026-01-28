package io.github.wladyslawpopov.kpager.core.paging.data

import kotlinx.coroutines.flow.StateFlow

interface Paginator<T : Any> {
    val itemsMap: StateFlow<Map<Int, T?>>
    val loadState: StateFlow<LoadState>
    val totalCount: StateFlow<Int>
    fun onPrefetch(pageToLoad: Int)

    suspend fun getItemById(id: Long): T?
    suspend fun updateItem(id: Long, updatedItem: T)
    fun reset(index: Int)
    fun close()
}
