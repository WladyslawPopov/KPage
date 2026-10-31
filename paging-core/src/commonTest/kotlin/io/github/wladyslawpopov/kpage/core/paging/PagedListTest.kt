package io.github.wladyslawpopov.kpage.core.paging

import io.github.wladyslawpopov.kpage.core.paging.data.state.PagedList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PagedListTest {

    @Test
    fun test_size_is_total_count_and_missing_positions_are_null() {
        val list = PagedList(size = 10_000_000, loaded = mapOf(0 to "a", 9_999_999 to "z"))

        assertEquals(10_000_000, list.size)
        assertEquals("a", list[0])
        assertEquals("z", list[9_999_999])
        assertNull(list[5_000_000])
    }

    @Test
    fun test_loaded_count_is_loaded_and_loaded_indices() {
        val list = PagedList(size = 100, loaded = mapOf(70 to "b", 3 to "a", 150 to "past the end"))

        assertEquals(2, list.loadedCount)
        assertTrue(list.isLoaded(3))
        assertFalse(list.isLoaded(4))
        assertFalse(list.isLoaded(1_000))
        assertEquals(listOf(3, 70), list.loadedIndices)
    }

    @Test
    fun test_get_outside_bounds_throws() {
        val list = PagedList(size = 3, loaded = mapOf(0 to "a"))

        assertFailsWith<IndexOutOfBoundsException> { list[3] }
        assertFailsWith<IndexOutOfBoundsException> { list[-1] }
    }

    @Test
    fun test_equality_compares_loaded_positions_not_every_index() {
        val a = PagedList(size = 1_000_000, loaded = mapOf(1 to "x", 2 to "y"))
        val same = PagedList(size = 1_000_000, loaded = mapOf(2 to "y", 1 to "x"))
        val otherItem = PagedList(size = 1_000_000, loaded = mapOf(1 to "x", 2 to "changed"))
        val otherSize = PagedList(size = 1_000_001, loaded = mapOf(1 to "x", 2 to "y"))
        val fewer = PagedList(size = 1_000_000, loaded = mapOf(1 to "x"))

        assertEquals(a, same)
        assertFalse(a == otherItem)
        assertFalse(a == otherSize)
        assertFalse(a == fewer)
    }
}
