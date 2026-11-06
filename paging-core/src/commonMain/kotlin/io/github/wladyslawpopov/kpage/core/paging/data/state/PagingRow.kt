package io.github.wladyslawpopov.kpage.core.paging.data.state

import io.github.wladyslawpopov.kpage.core.paging.data.effect.PagingIndexedItem
import io.github.wladyslawpopov.kpage.core.paging.data.effect.PagingRowSource

/** One row of a decorated list: a server position (an item or a placeholder) or a decoration. */
sealed interface PagingRow<out T : Any, out D : Any> : PagingIndexedItem {

    /** The server position [serverIndex]; [item] is null while it is not loaded (a placeholder). */
    data class Item<T : Any>(override val serverIndex: Int, val item: T?) : PagingRow<T, Nothing>

    /** An app-defined row placed right before the server position [beforeIndex]. */
    data class Decoration<D : Any>(val value: D, val beforeIndex: Int) : PagingRow<Nothing, D> {
        override val serverIndex: Int? get() = null
    }
}

/**
 * Declares the decorations of a list. Every decoration is placed before a server position
 * (`0..items.size`, where `items.size` is after the last item). Decorations at the same position
 * keep the order of the calls.
 */
interface DecorationScope<T : Any, D : Any> {
    val items: PagedList<T>

    /** A row before the first position. */
    fun header(value: D)

    /** A row after the last position. */
    fun footer(value: D)

    /**
     * A row before the server position [beforeIndex], for example a local item. A position outside
     * `0..items.size` (computed from an older snapshot) is clamped to it instead of failing.
     */
    fun insert(beforeIndex: Int, value: D)

    /** A row between two neighbouring positions, only when both of them are loaded. */
    fun between(rule: (prev: T, next: T) -> D?)
}

/**
 * Builds the rows of the UI: every server position plus the decorations,
 * `rowCount = items.size + decorations`. Only loaded items and decorations are visited and stored,
 * so the cost does not depend on the total count.
 */
fun <T : Any, D : Any> PagedList<T>.decorate(
    block: DecorationScope<T, D>.() -> Unit
): PagingRows<T, D> {
    val scope = DecorationBuilder<T, D>(this)
    scope.block()
    return PagingRows(this, scope.decorations.sortedBy { it.beforeIndex })
}

/** The rows of the UI without decorations: one row per server position. */
fun <T : Any> PagedList<T>.rows(): PagingRows<T, Nothing> = PagingRows(this, emptyList())

/**
 * The rows a list UI draws: every server position (an item or a placeholder) and the decorations,
 * sorted by position. A row is created when it is read, so a list as long as the total count costs
 * only its loaded items and decorations.
 *
 * Not a `List` on purpose: Kotlin/Native turns every `List` into an `NSArray`, so a UI outside Kotlin
 * (SwiftUI, UIKit) would get a converted copy. This object crosses by reference and is read row by row.
 * Compose reads it the same way: `items(count = rows.rowCount) { rows[it] }`.
 *
 * Equality compares the items and the decorations, not every row.
 */
class PagingRows<out T : Any, out D : Any> internal constructor(
    // Only read, never written: safe for the covariant T and D. Internal so equals can compare
    // the rows of another instance.
    internal val items: PagedList<@UnsafeVariance T>,
    internal val decorations: List<PagingRow.Decoration<@UnsafeVariance D>>,
) : PagingRowSource {

    // UI index of every decoration: its position plus the decorations before it.
    private val decorationRows = IntArray(decorations.size) { decorations[it].beforeIndex + it }

    override val rowCount: Int = items.size + decorations.size

    operator fun get(index: Int): PagingRow<T, D> {
        checkIndex(index)
        val decoration = decorationAt(index)
        if (decoration >= 0) return decorations[decoration]
        val serverIndex = index - decorationsBefore(index)
        return PagingRow.Item(serverIndex, items[serverIndex])
    }

    override fun serverIndexAt(row: Int): Int? {
        if (row !in 0 until rowCount || decorationAt(row) >= 0) return null
        return row - decorationsBefore(row)
    }

    /**
     * Keys for a lazy list, safe when the server shifts its pages. An item keeps the key of
     * [itemKey], so rows stay stable when new items shift the list; an item that briefly repeats on a
     * second position gets the position added, so keys never collide. Placeholders are keyed by
     * their position, decorations by [decorationKey] or by their position and order.
     *
     * Keys made here are strings; give [itemKey] a key a Bundle can save too (a string or a number).
     * Build it once per rows (`remember(rows) { rows.keys { it.id } }`): it walks the loaded items.
     */
    fun keys(
        itemKey: (T) -> Any,
        decorationKey: ((decoration: D, beforeIndex: Int) -> Any)? = null,
    ): (index: Int) -> Any {
        val seen = HashSet<Any>()
        val repeats = items.loadedIndices.filterTo(HashSet()) { index -> !seen.add(itemKey(items[index]!!)) }
        return { index ->
            checkIndex(index)
            val decorationIndex = decorationAt(index)
            if (decorationIndex >= 0) {
                val decoration = decorations[decorationIndex]
                decorationKey?.invoke(decoration.value, decoration.beforeIndex)
                    ?: "decoration:${decoration.beforeIndex}:${decorationIndex - firstDecorationAt(decoration.beforeIndex)}"
            } else {
                val serverIndex = index - decorationsBefore(index)
                val item = items[serverIndex]
                when {
                    item == null -> "slot:$serverIndex"
                    serverIndex in repeats -> "${itemKey(item)}#$serverIndex"
                    else -> itemKey(item)
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean =
        other === this || other is PagingRows<*, *> && items == other.items && decorations == other.decorations

    // Consistent with equals without walking the list: equal rows have the same size and decorations.
    override fun hashCode(): Int = 31 * items.size + decorations.hashCode()

    override fun toString(): String = "PagingRows(rowCount=$rowCount, decorations=${decorations.size})"

    private fun checkIndex(index: Int) {
        if (index !in 0 until rowCount) throw IndexOutOfBoundsException("index: $index, rowCount: $rowCount")
    }

    // Decorations before the UI row [index]; when the row is a decoration, it is the next one.
    private fun decorationsBefore(index: Int): Int = decorationRows.lowerBound(index)

    // Index in [decorations] of the decoration at the UI row [index], or -1 for a server position.
    private fun decorationAt(index: Int): Int {
        val before = decorationsBefore(index)
        return if (before < decorationRows.size && decorationRows[before] == index) before else -1
    }

    // Index in [decorations] of the first decoration placed before [beforeIndex].
    private fun firstDecorationAt(beforeIndex: Int): Int {
        var low = 0
        var high = decorations.size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (decorations[mid].beforeIndex < beforeIndex) low = mid + 1 else high = mid
        }
        return low
    }

    private fun IntArray.lowerBound(value: Int): Int {
        var low = 0
        var high = size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (this[mid] < value) low = mid + 1 else high = mid
        }
        return low
    }
}

private class DecorationBuilder<T : Any, D : Any>(override val items: PagedList<T>) : DecorationScope<T, D> {
    val decorations = ArrayList<PagingRow.Decoration<D>>()

    override fun header(value: D) = insert(0, value)

    override fun footer(value: D) = insert(items.size, value)

    override fun insert(beforeIndex: Int, value: D) {
        decorations += PagingRow.Decoration(value, beforeIndex.coerceIn(0, items.size))
    }

    override fun between(rule: (prev: T, next: T) -> D?) {
        val loaded = items.loadedIndices
        for (i in 1 until loaded.size) {
            val prev = loaded[i - 1]
            val next = loaded[i]
            if (next != prev + 1) continue
            rule(items[prev]!!, items[next]!!)?.let { decorations += PagingRow.Decoration(it, next) }
        }
    }
}
