package io.github.wladyslawpopov.kpager.core.paging.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.wladyslawpopov.kpager.cache.PagingDataBase
import io.github.wladyslawpopov.kpager.core.paging.StablePaginator
import java.util.Properties

object TestPaginatorFactory {

    fun createInMemoryDriver(): SqlDriver {
        val properties = Properties().apply {
            setProperty("busy_timeout", "5000")
        }
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, properties)

        PagingDataBase.Schema.create(driver)
        return driver
    }

    fun createPaginator(
        api: FakeApi,
        driver: SqlDriver
    ): StablePaginator<FakeItem> {
        return StablePaginator(
            serializer = FakeItem.serializer(),
            queryKey = "test_query",
            config = PaginatorConfig(
                pageSize = api.pageSize
            ),
            getPage = { page -> api.getPage(page) },
            idExtractor = { it.id },
            driver = driver
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
