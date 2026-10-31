package io.github.wladyslawpopov.kpage.core.paging.data

import io.github.wladyslawpopov.kpage.cache.PagingDatabase

/** Database helpers for tests: an in-memory database and seeding of lists, items and pages. */
object TestPaginatorFactory {

    const val QUERY_KEY = "test_query"

    val ITEM_TYPE: String = FakeItem.serializer().descriptor.serialName

    fun createInMemoryDriver(): PagingDatabase {
        val driver = createTestDriver()
        return PagingDatabase(driver)
    }

    fun itemJson(id: String, title: String) = """{"id":"$id","title":"$title"}"""

    fun seedQuery(db: PagingDatabase, queryKey: String, totalCount: Int, pageSize: Int) {
        db.queryMetaQueries.insertIfAbsent(queryKey, pageSize.toLong())
        db.queryMetaQueries.setTotalCount(totalCount.toLong(), queryKey)
    }

    /** Stores the item and puts it on [position] of the list. */
    fun seedItem(
        db: PagingDatabase,
        queryKey: String,
        position: Int,
        id: String,
        json: String = itemJson(id, "Old Item $position"),
    ) {
        db.itemRawQueries.upsertItem(ITEM_TYPE, id, json)
        db.listingEntryQueries.upsertEntry(queryKey, position.toLong(), ITEM_TYPE, id)
    }

    fun prePopulateDatabase(database: PagingDatabase, pages: Int, pageSize: Int) {
        database.transaction {
            seedQuery(database, QUERY_KEY, pages * pageSize, pageSize)
            for (page in 0 until pages) {
                database.pageMetadataQueries.upsertPageMetadata(QUERY_KEY, page.toLong(), page < pages - 1)
                for (i in 0 until pageSize) {
                    val globalIndex = page * pageSize + i
                    seedItem(database, QUERY_KEY, globalIndex, "id_$globalIndex")
                }
            }
        }
    }
}

/** Page numbers that currently have at least one cached entry. */
fun PagingDatabase.existingPages(pageSize: Int, queryKey: String = TestPaginatorFactory.QUERY_KEY): List<Long> =
    listingEntryQueries.getExistingPageNumbers(pageSize.toLong(), queryKey).executeAsList()
