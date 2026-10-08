package io.github.wladyslawpopov.kpager.core.paging.data

const val PAGE_SIZE = 60
const val DATABASE_NAME = "PagingDataBase"


/** prefetchDistance is the number of neighboring pages to load and retain in each direction. */
data class PaginatorConfig(
    val pageSize: Int = PAGE_SIZE,
    val prefetchDistance: Int = 1,
    val initialPageKey: Int = 0,
    val maxPagesInDb: Int = 3
)
