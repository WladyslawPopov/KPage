package io.github.wladyslawpopov.kpager.core.paging.data

data class PagerPayload<T>(
    val totalCount: Int,
    val isMore: Boolean,
    val objects: List<T>
)
