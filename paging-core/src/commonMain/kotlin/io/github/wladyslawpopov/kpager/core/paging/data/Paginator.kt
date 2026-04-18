package io.github.wladyslawpopov.kpager.core.paging.data

import kotlinx.coroutines.flow.StateFlow

interface Paginator<T : Any> {
    val itemsMap: StateFlow<Map<Int, T?>>
    val loadState: StateFlow<LoadState>
    val totalCount: StateFlow<Int>

    suspend fun getItemById(id: String): T?
    suspend fun updateItem(id: String, updatedItem: T)

    fun onPrefetch(pageToLoad: Int)
    fun reset(index: Int)
    fun close()
}
