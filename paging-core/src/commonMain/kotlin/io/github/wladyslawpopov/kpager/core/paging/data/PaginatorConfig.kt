package io.github.wladyslawpopov.kpager.core.paging.data

const val PAGE_SIZE = 60
const val DATABASE_NAME = "PagingDatabase"


data class PaginatorConfig(
    val pageSize: Int = PAGE_SIZE,
    val prefetchDistance: Int = 1,
    val initialPageKey: Int = 0,
    val maxPagesInDb: Int = 3
)
