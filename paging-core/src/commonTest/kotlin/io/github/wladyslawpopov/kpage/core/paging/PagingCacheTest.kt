package io.github.wladyslawpopov.kpage.core.paging

import io.github.wladyslawpopov.kpage.core.paging.data.TestPaginatorFactory
import io.github.wladyslawpopov.kpage.core.paging.data.TestPaginatorFactory.ITEM_TYPE
import io.github.wladyslawpopov.kpage.core.paging.data.TestPaginatorFactory.QUERY_KEY
import io.github.wladyslawpopov.kpage.core.paging.data.FakeItem
import io.github.wladyslawpopov.kpage.core.paging.data.cache.clearCache
import io.github.wladyslawpopov.kpage.core.paging.data.cache.updateItem
import io.github.wladyslawpopov.kpage.core.paging.data.existingPages
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PagingCacheTest {

    @Test
    fun test_clear_cache_removes_everything() = runTest {
        val database = TestPaginatorFactory.createInMemoryDriver()
        TestPaginatorFactory.prePopulateDatabase(database, pages = 2, pageSize = 3)
        assertNotNull(database.itemRawQueries.selectById(ITEM_TYPE, "id_0").executeAsOneOrNull())
        assertTrue(database.existingPages(3).isNotEmpty())

        database.clearCache()

        assertNull(database.itemRawQueries.selectById(ITEM_TYPE, "id_0").executeAsOneOrNull())
        assertEquals(emptyList(), database.existingPages(3))
        assertNull(database.pageMetadataQueries.getPageMetadata(QUERY_KEY, 0).executeAsOneOrNull())
        assertNull(database.queryMetaQueries.selectByKey(QUERY_KEY).executeAsOneOrNull())
    }

    @Test
    fun test_clear_cache_of_one_query_keeps_the_others_and_shared_items() = runTest {
        val database = TestPaginatorFactory.createInMemoryDriver()
        TestPaginatorFactory.prePopulateDatabase(database, pages = 1, pageSize = 3)
        TestPaginatorFactory.seedQuery(database, "other", totalCount = 2, pageSize = 3)
        database.pageMetadataQueries.upsertPageMetadata("other", 0, false)
        TestPaginatorFactory.seedItem(database, "other", 0, "id_0")
        TestPaginatorFactory.seedItem(database, "other", 1, "other_only")

        database.clearCache("other")

        assertNull(database.queryMetaQueries.selectByKey("other").executeAsOneOrNull())
        assertEquals(0L, database.listingEntryQueries.countEntries("other").executeAsOne())
        assertNull(database.pageMetadataQueries.getPageMetadata("other", 0).executeAsOneOrNull())
        assertNull(
            database.itemRawQueries.selectById(ITEM_TYPE, "other_only").executeAsOneOrNull(),
            "An item used only by the cleared list must be deleted",
        )
        assertNotNull(
            database.itemRawQueries.selectById(ITEM_TYPE, "id_0").executeAsOneOrNull(),
            "An item still used by another list must stay",
        )
        assertEquals(3L, database.listingEntryQueries.countEntries(QUERY_KEY).executeAsOne())
        assertNotNull(database.queryMetaQueries.selectByKey(QUERY_KEY).executeAsOneOrNull())
    }

    @Test
    fun test_update_item_without_paginator_changes_cached_item_only() = runTest {
        val database = TestPaginatorFactory.createInMemoryDriver()
        TestPaginatorFactory.prePopulateDatabase(database, pages = 1, pageSize = 3)

        val updated = database.updateItem(FakeItem.serializer(), "id_1") { it.copy(title = "Edited") }

        assertEquals(FakeItem("id_1", "Edited"), updated)
        val json = database.itemRawQueries.selectById(ITEM_TYPE, "id_1").executeAsOne().json
        assertTrue("Edited" in json)
        assertNull(database.updateItem(FakeItem.serializer(), "missing") { it.copy(title = "Edited") })
    }

    @Test
    fun test_update_item_with_unreadable_json_returns_null_and_keeps_it() = runTest {
        val database = TestPaginatorFactory.createInMemoryDriver()
        TestPaginatorFactory.prePopulateDatabase(database, pages = 1, pageSize = 3)
        database.itemRawQueries.updateJson("not json", ITEM_TYPE, "id_1")

        assertNull(database.updateItem(FakeItem.serializer(), "id_1") { it.copy(title = "Edited") })

        assertEquals("not json", database.itemRawQueries.selectById(ITEM_TYPE, "id_1").executeAsOne().json)
    }
}
