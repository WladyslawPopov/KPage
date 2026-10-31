package io.github.wladyslawpopov.kpage.core.paging.data

import io.github.wladyslawpopov.kpage.core.paging.data.paginator.PagerPayload
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A server whose answers a test controls: a page can be held back ([hold] / [release]), made to
 * fail, and the list behind it can change. Titles carry [version] so a test can tell fresh data
 * from cached data. Pages are numbered from [firstPage].
 */
class GatedServer(var ids: List<String>, val pageSize: Int, val firstPage: Int = 0) {
    var version = "New"
    var failAll = false
    // False: an API without a total count (PagerPayload.totalCount = null).
    var reportsTotal = true
    val failing = mutableSetOf<Int>()

    private val gates = mutableMapOf<Int, CompletableDeferred<Unit>>()
    private val lock = Mutex()
    private var log = emptyList<Int>()

    /** Pages requested so far, in order (the number the paginator passed to `getPage`). */
    val requests: List<Int> get() = log

    fun hold(vararg pages: Int) = pages.forEach { gates[it] = CompletableDeferred() }

    fun release(vararg pages: Int) = pages.forEach { gates.remove(it)?.complete(Unit) }

    fun clearRequests() {
        log = emptyList()
    }

    suspend fun getPage(page: Int): PagerPayload<FakeItem> {
        lock.withLock { log = log + page }
        gates[page]?.await()
        if (failAll || page in failing) throw IllegalStateException("Server error on page $page")
        val from = (page - firstPage) * pageSize
        val slice = if (from < 0) emptyList() else ids.drop(from).take(pageSize)
        return PagerPayload(
            totalCount = if (reportsTotal) ids.size else null,
            isMore = from + slice.size < ids.size,
            objects = slice.map { FakeItem(it, "$version $it") },
        )
    }
}
