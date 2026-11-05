package io.github.wladyslawpopov.kpage.demo.data.favorites

import io.github.wladyslawpopov.kpage.demo.data.db.DemoDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/** The ids of the favorite images in the demo's database, newest first. */
internal class FavoritesStore(db: DemoDatabase) {
    private val queries = db.favoriteQueries

    suspend fun add(id: String) {
        io { queries.insert(id) }
    }

    suspend fun remove(id: String) {
        io { queries.delete(id) }
    }

    suspend fun removeAll(ids: Collection<String>) {
        io { queries.transaction { ids.forEach { queries.delete(it) } } }
    }

    suspend fun allIds(): List<String> = io { queries.selectAll().executeAsList() }

    suspend fun count(): Int = io { queries.count().executeAsOne().toInt() }

    suspend fun page(offset: Int, limit: Int): List<String> =
        io { queries.selectPage(limit.toLong(), offset.toLong()).executeAsList() }

    /** Which of [ids] are favorite. */
    suspend fun favoriteIds(ids: Collection<String>): Set<String> =
        if (ids.isEmpty()) emptySet() else io { queries.selectExisting(ids).executeAsList().toSet() }

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }
}
