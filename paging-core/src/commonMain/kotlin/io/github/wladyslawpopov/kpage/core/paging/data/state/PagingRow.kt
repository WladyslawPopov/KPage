package io.github.wladyslawpopov.kpage.core.paging.data.state

import io.github.wladyslawpopov.kpage.core.paging.data.effect.PagingIndexedItem

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
 * Builds the UI list: every server position plus the decorations, `size = items.size + decorations`.
 * Only loaded items and decorations are visited and stored, so the cost does not depend on the
 * total count; rows are created on access.
 */
fun <T : Any, D : Any> PagedList<T>.decorate(
    block: DecorationScope<T, D>.() -> Unit
): List<PagingRow<T, D>> {
    val scope = DecorationBuilder<T, D>(this)
    scope.block()
    return DecoratedList(this, scope.decorations.sortedBy { it.beforeIndex })
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

/**
 * The merge of the server positions and the decorations, sorted by position. Equality compares the
 * items and the decorations, not every row; [hashCode] follows the [List] contract and walks the list.
 */
private class DecoratedList<T : Any, D : Any>(
    private val items: PagedList<T>,
    private val decorations: List<PagingRow.Decoration<D>>,
) : AbstractList<PagingRow<T, D>>(), RandomAccess {

    // UI index of every decoration: its position plus the decorations before it.
    private val decorationRows = IntArray(decorations.size) { decorations[it].beforeIndex + it }

    override val size: Int = items.size + decorations.size

    override fun get(index: Int): PagingRow<T, D> {
        if (index !in indices) throw IndexOutOfBoundsException("index: $index, size: $size")
        // Decorations before this row; when the row is a decoration, it is the next one.
        val before = decorationRows.lowerBound(index)
        if (before < decorationRows.size && decorationRows[before] == index) return decorations[before]
        val serverIndex = index - before
        return PagingRow.Item(serverIndex, items[serverIndex])
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other is DecoratedList<*, *>) return items == other.items && decorations == other.decorations
        return super.equals(other)
    }

    override fun hashCode(): Int = super.hashCode()

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
