package io.github.wladyslawpopov.kpage.core.paging.data.state

/**
 * A read-only list as long as the whole server list: `size` is the total count, `get(i)` is the item
 * at server position `i`, or `null` while that position is not loaded (a placeholder).
 *
 * Only loaded positions are stored, so a list of a million items with a few hundred loaded costs
 * a few hundred entries. Use [loadedCount] and [isLoaded] instead of scanning the list.
 *
 * Equality compares the size and the loaded positions, not every index, so comparing two snapshots
 * is proportional to the number of loaded items. [hashCode] follows the [List] contract and walks
 * the whole list; do not use it as a key.
 */
class PagedList<T : Any> internal constructor(
    override val size: Int,
    loaded: Map<Int, T>,
) : AbstractList<T?>(), RandomAccess {

    private val loaded: Map<Int, T> = loaded.filterKeys { it in 0 until size }

    /** Number of positions that currently hold an item. */
    val loadedCount: Int get() = loaded.size

    /** Loaded positions in ascending order; use it to walk the loaded items without scanning the list. */
    val loadedIndices: List<Int> by lazy { this.loaded.keys.sorted() }

    /** True if the item at [index] is loaded; false for placeholders and out-of-range indexes. */
    fun isLoaded(index: Int): Boolean = loaded.containsKey(index)

    override fun get(index: Int): T? {
        if (index < 0 || index >= size) throw IndexOutOfBoundsException("index: $index, size: $size")
        return loaded[index]
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other is PagedList<*>) return size == other.size && loaded == other.loaded
        return super.equals(other)
    }

    override fun hashCode(): Int = super.hashCode()

    internal companion object {
        fun <T : Any> empty(): PagedList<T> = PagedList(0, emptyMap())
    }
}

/**
 * Builds a [PagedList] of [size] positions with [loaded] items, for previews, tests and fake
 * paginators. Positions outside `0 until size` are ignored.
 */
fun <T : Any> pagedListOf(size: Int, loaded: Map<Int, T> = emptyMap()): PagedList<T> = PagedList(size, loaded)
