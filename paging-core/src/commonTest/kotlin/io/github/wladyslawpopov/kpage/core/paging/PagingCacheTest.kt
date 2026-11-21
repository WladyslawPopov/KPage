package io.github.wladyslawpopov.kpage.core.paging

import io.github.wladyslawpopov.kpage.core.paging.data.TestPaginatorFactory
import io.github.wladyslawpopov.kpage.core.paging.data.TestPaginatorFactory.ITEM_TYPE
import io.github.wladyslawpopov.kpage.core.paging.data.TestPaginatorFactory.QUERY_KEY
import io.github.wladyslawpopov.kpage.core.paging.data.FakeItem
import io.github.wladyslawpopov.kpage.core.paging.data.cache.PagingCacheStats
import io.github.wladyslawpopov.kpage.core.paging.data.cache.PagingStore
import io.github.wladyslawpopov.kpage.core.paging.data.cache.clearCache
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
    fun test_item_cache_reads_and_changes_the_cached_item_only() = runTest {
        val database = TestPaginatorFactory.createInMemoryDriver()
        TestPaginatorFactory.prePopulateDatabase(database, pages = 1, pageSize = 3)
        val items = PagingStore(database).items(FakeItem.serializer())

        val updated = items.update("id_1") { it.copy(title = "Edited") }

        assertEquals(FakeItem("id_1", "Edited"), updated)
        assertEquals(FakeItem("id_1", "Edited"), items.get("id_1"))
        val json = database.itemRawQueries.selectById(ITEM_TYPE, "id_1").executeAsOne().json
        assertTrue("Edited" in json)
        assertNull(items.update("missing") { it.copy(title = "Edited") })
        assertNull(items.get("missing"))
    }

    @Test
    fun test_item_cache_with_unreadable_json_returns_null_and_keeps_it() = runTest {
        val database = TestPaginatorFactory.createInMemoryDriver()
        TestPaginatorFactory.prePopulateDatabase(database, pages = 1, pageSize = 3)
        database.itemRawQueries.updateJson("not json", ITEM_TYPE, "id_1")
        val items = PagingStore(database).items(FakeItem.serializer())

        assertNull(items.update("id_1") { it.copy(title = "Edited") })
        assertNull(items.get("id_1"))

        assertEquals("not json", database.itemRawQueries.selectById(ITEM_TYPE, "id_1").executeAsOne().json)
    }

    @Test
    fun test_store_stats_before_and_after_clear() = runTest {
        val database = TestPaginatorFactory.createInMemoryDriver()
        TestPaginatorFactory.prePopulateDatabase(database, pages = 2, pageSize = 3)
        val store = PagingStore(database)

        val before = store.stats()
        assertEquals(1, before.lists)
        assertEquals(6, before.items)
        assertTrue(before.dataBytes > 0)

        store.clear()

        assertEquals(PagingCacheStats(lists = 0, items = 0, dataBytes = 0), store.stats())
    }
}
