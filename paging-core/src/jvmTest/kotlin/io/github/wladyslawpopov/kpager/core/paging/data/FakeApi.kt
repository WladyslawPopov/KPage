package io.github.wladyslawpopov.kpager.core.paging.data

import kotlinx.coroutines.delay

class FakeApi {
    var shouldFail = false
    var totalPages = 5
    var pageSize = 20
    var networkDelayMs = 100L

    suspend fun getPage(page: Int): PagerPayload<FakeItem> {
        if (shouldFail) throw IllegalStateException("ServerErrorException")
        
        delay(networkDelayMs)
        
        val isMore = page < (totalPages - 1)
        val items = if (page < totalPages) {
            List(pageSize) { index -> 
                val globalIndex = page * pageSize + index
                FakeItem(id = "id_$globalIndex", title = "Item $globalIndex") 
            }
        } else emptyList()

        return PagerPayload(objects = items, isMore = isMore, totalCount = totalPages * pageSize)
    }
}
