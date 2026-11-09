package io.github.wladyslawpopov.kpage.core.paging

import io.github.wladyslawpopov.kpage.core.paging.data.state.PagedList
import io.github.wladyslawpopov.kpage.core.paging.data.state.PagingRow
import io.github.wladyslawpopov.kpage.core.paging.data.state.PagingRows
import io.github.wladyslawpopov.kpage.core.paging.data.state.decorate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PagingRowTest {

    private fun item(serverIndex: Int, value: String?) = PagingRow.Item(serverIndex, value)

    private fun decor(value: String, beforeIndex: Int) = PagingRow.Decoration(value, beforeIndex)

    private fun <T : Any, D : Any> PagingRows<T, D>.all() = List(rowCount) { this[it] }

    @Test
    fun empty_rows_have_no_rows_and_equal_the_rows_of_an_empty_list() {
        val rows = PagingRows.empty<String>()

        assertEquals(0, rows.rowCount)
        assertEquals(rows, PagedList<String>(size = 0, loaded = emptyMap()).decorate<String, String> {})
    }

    @Test
    fun header_insert_and_footer_are_merged_with_server_positions() {
        val items = PagedList(size = 4, loaded = mapOf(0 to "a", 1 to "b", 3 to "d"))

        val rows = items.decorate<String, String> {
            header("H")
            insert(beforeIndex = 2, "I")
            footer("F")
        }

        assertEquals(
            listOf(decor("H", 0), item(0, "a"), item(1, "b"), decor("I", 2), item(2, null), item(3, "d"), decor("F", 4)),
            rows.all(),
        )
        assertEquals(listOf(null, 0, 1, null, 2, 3, null), rows.all().map { it.serverIndex })
    }

    @Test
    fun between_is_evaluated_only_for_loaded_neighbours() {
        val items = PagedList(size = 6, loaded = mapOf(0 to "a", 1 to "b", 3 to "d", 4 to "e"))

        val rows = items.decorate<String, String> {
            between { prev, next -> "$prev|$next" }
        }

        val separators = rows.all().filterIsInstance<PagingRow.Decoration<String>>()
        assertEquals(listOf(decor("a|b", 1), decor("d|e", 4)), separators, "No separator next to a placeholder")
        assertEquals(6 + 2, rows.rowCount)
    }

    @Test
    fun decorations_at_the_same_position_keep_the_order_of_the_calls() {
        val items = PagedList(size = 2, loaded = mapOf(0 to "a", 1 to "b"))

        val rows = items.decorate<String, String> {
            insert(beforeIndex = 1, "first")
            between { _, _ -> "between" }
            insert(beforeIndex = 1, "last")
        }

        assertEquals(listOf("a", "first", "between", "last", "b"), rows.all().map {
            when (it) {
                is PagingRow.Item -> it.item
                is PagingRow.Decoration -> it.value
            }
        })
    }

    @Test
    fun a_huge_list_costs_only_its_loaded_items_and_decorations() {
        val size = Int.MAX_VALUE / 2
        val items = PagedList(size = size, loaded = mapOf(0 to "a", 1 to "b", size - 1 to "z"))

        val rows = items.decorate<String, String> {
            header("H")
            between { _, _ -> "S" }
            footer("F")
        }

        assertEquals(size + 3, rows.rowCount)
        assertEquals(item(0, "a"), rows[1])
        assertEquals(decor("S", 1), rows[2])
        assertEquals(item(1_000_000, null), rows[1_000_002])
        assertEquals(item(size - 1, "z"), rows[size + 1])
        assertEquals(decor("F", size), rows[size + 2])
        assertTrue(rows == items.decorate<String, String> {
            header("H")
            between { _, _ -> "S" }
            footer("F")
        }, "Equality does not walk the whole list")
    }

    @Test
    fun insert_outside_the_list_is_clamped_instead_of_failing() {
        val items = PagedList(size = 2, loaded = emptyMap<Int, String>())

        val rows = items.decorate<String, String> {
            insert(5, "late")
            insert(-1, "early")
        }

        assertEquals(decor("early", 0), rows[0])
        assertEquals(decor("late", 2), rows[rows.rowCount - 1])
    }

    @Test
    fun server_index_of_a_row_skips_decorations_and_rows_out_of_range() {
        val rows = PagedList(size = 3, loaded = emptyMap<Int, String>()).decorate<String, String> {
            header("H")
            insert(beforeIndex = 2, "I")
        }

        assertEquals(listOf(null, 0, 1, null, 2), List(rows.rowCount) { rows.serverIndexAt(it) })
        assertEquals(null, rows.serverIndexAt(-1))
        assertEquals(null, rows.serverIndexAt(rows.rowCount))
    }

    @Test
    fun keys_keep_the_item_key_and_add_the_position_only_to_repeats() {
        // "a" repeats on position 2 after a server shift; position 3 is a placeholder.
        val items = PagedList(size = 4, loaded = mapOf(0 to "a", 1 to "b", 2 to "a"))
        val rows = items.decorate<String, String> {
            header("H")
            header("H2")
        }

        val key = rows.keys(itemKey = { it })

        assertEquals(
            listOf("decoration:0:0", "decoration:0:1", "a", "b", "a#2", "slot:3"),
            List(rows.rowCount) { key(it) },
        )
    }
}
