package io.github.wladyslawpopov.kpager.core.paging.data

import io.github.wladyslawpopov.kpager.cache.PagingDataBase
import io.github.wladyslawpopov.kpager.core.paging.StablePaginator
import io.github.wladyslawpopov.kpager.core.paging.common.getDriver

object TestPaginatorFactory {

    fun createInMemoryDriver(): PagingDataBase {
        val driver = getDriver()
        return PagingDataBase(driver)
    }

    fun createPaginator(
        api: FakeApi,
        db: PagingDataBase
    ): StablePaginator<FakeItem> {
        return StablePaginator(
            serializer = FakeItem.serializer(),
            queryKey = "test_query",
            config = PaginatorConfig(
                pageSize = api.pageSize
            ),
            getPage = { page -> api.getPage(page) },
            idExtractor = { it.id },
            db = db
        )
    }

    fun prePopulateDatabase(database: PagingDataBase, pages: Int, pageSize: Int) {
        val now = 1000L
        for (page in 0 until pages) {
            val nextPage = if (page < pages - 1) (page + 1).toString() else null
            database.pageMetadataQueries.insertPageMetadataIfAbsent("test_query", page.toLong(), nextPage, (pages * pageSize).toLong())
            for (i in 0 until pageSize) {
                val globalIndex = page * pageSize + i
                val id = "id_$globalIndex"
                val itemJson = """{"id":"$id","title":"Old Item $globalIndex"}"""
                database.itemRawQueries.insertItemIfAbsent(id, itemJson, now)
                database.listingEntryQueries.insertEntryIfAbsent("test_query", id, globalIndex.toLong(), page.toLong())
            }
        }
    }
}
