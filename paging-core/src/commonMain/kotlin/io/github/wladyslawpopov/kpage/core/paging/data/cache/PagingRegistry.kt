package io.github.wladyslawpopov.kpage.core.paging.data.cache

import io.github.wladyslawpopov.kpage.cache.PagingDatabase
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * The live paginators of every database: [PagingStore.invalidate] and [PagingStore.clear] reach them
 * here. A paginator is registered while its scope lives.
 *
 * One per process, keyed by the database: the paginators and the store meet here without holding
 * references to each other, so the app wires nothing.
 */
@OptIn(ExperimentalAtomicApi::class)
internal object LivePaginators {

    /** Identity matters: two paginators of one group are two entries. */
    class Entry(val group: String?, val refresh: () -> Unit)

    private val live = AtomicReference(emptyMap<PagingDatabase, Set<Entry>>())

    fun register(db: PagingDatabase, entry: Entry) {
        while (true) {
            val current = live.load()
            if (live.compareAndSet(current, current + (db to current[db].orEmpty() + entry))) return
        }
    }

    fun unregister(db: PagingDatabase, entry: Entry) {
        while (true) {
            val current = live.load()
            val entries = current[db].orEmpty() - entry
            val next = if (entries.isEmpty()) current - db else current + (db to entries)
            if (live.compareAndSet(current, next)) return
        }
    }

    /** Asks the live paginators of [groups] to refresh; null asks every live paginator of [db]. */
    fun refresh(db: PagingDatabase, groups: Set<String>?) {
        live.load()[db].orEmpty()
            .filter { groups == null || it.group in groups }
            .forEach { it.refresh() }
    }
}

/**
 * Item edits of every database, so that a page load that started before an edit does not bring the
 * old version back, whichever paginator or caller made the edit.
 *
 * Every load takes a tick when it starts; an edit takes the next one. A page keeps the cached JSON of
 * an item edited after its load started. An edit matters only while a load older than it is in
 * flight, so each paginator reports its oldest load and older edits are dropped.
 *
 * One per process, keyed by the database, like [LivePaginators]: every paginator and store of the
 * database shares it.
 */
@OptIn(ExperimentalAtomicApi::class)
internal object ItemEdits {

    private data class ItemKey(val itemType: String, val id: String)

    // loads: the start tick of the oldest load in flight, per paginator.
    private class State(
        val tick: Long = 0,
        val edits: Map<ItemKey, Long> = emptyMap(),
        val loads: Map<Any, Long> = emptyMap(),
    )

    private val states = AtomicReference(emptyMap<PagingDatabase, State>())

    private inline fun <R> change(db: PagingDatabase, block: (State) -> Pair<State, R>): R {
        while (true) {
            val current = states.load()
            val (next, result) = block(current[db] ?: State())
            val unused = next.loads.isEmpty() && next.edits.isEmpty()
            if (states.compareAndSet(current, if (unused) current - db else current + (db to next))) return result
        }
    }

    /**
     * A load of [owner] starts; returns its tick. Called at once, so an edit made after this moment is
     * kept even before the owner reports again with [settle].
     */
    fun startLoad(db: PagingDatabase, owner: Any): Long = change(db) { state ->
        val oldest = minOf(state.loads[owner] ?: state.tick, state.tick)
        State(state.tick, state.edits, state.loads + (owner to oldest)) to state.tick
    }

    /**
     * Reports the start tick of the oldest load of [owner] still in flight, or null when it has none
     * (also when it stops). Edits no load in flight is older than are dropped.
     */
    fun settle(db: PagingDatabase, owner: Any, oldest: Long?) = change(db) { state ->
        val loads = if (oldest == null) state.loads - owner else state.loads + (owner to oldest)
        val min = loads.values.minOrNull()
        val edits = if (min == null) emptyMap() else state.edits.filterValues { it > min }
        State(state.tick, edits, loads) to Unit
    }

    /**
     * The item was changed. Called inside the transaction of the change: a page written after it
     * commits sees the edit.
     */
    fun markEdited(db: PagingDatabase, itemType: String, id: String) = change(db) { state ->
        // Without loads in flight nothing can bring the old version back.
        if (state.loads.isEmpty()) return@change state to Unit
        val tick = state.tick + 1
        State(tick, state.edits + (ItemKey(itemType, id) to tick), state.loads) to Unit
    }

    /** True if the item was edited after a load that started at [startTick]. */
    fun isEditedAfter(db: PagingDatabase, itemType: String, id: String, startTick: Long): Boolean {
        val edit = states.load()[db]?.edits?.get(ItemKey(itemType, id)) ?: return false
        return edit > startTick
    }
}
