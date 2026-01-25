package io.github.wladyslawpopov.kpager.core.paging.data

import kotlinx.coroutines.flow.StateFlow

data class PagingResult<T : Any>(
    val itemsMap: StateFlow<Map<Int, T?>>,
    val loadState: StateFlow<LoadState>,
    val totalCount: StateFlow<Int>,
    val paginator: Paginator<T>
)
