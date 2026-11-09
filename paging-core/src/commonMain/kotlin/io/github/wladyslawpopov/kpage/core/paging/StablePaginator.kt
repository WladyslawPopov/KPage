package io.github.wladyslawpopov.kpage.core.paging

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.github.wladyslawpopov.kpage.cache.PagingDatabase
import io.github.wladyslawpopov.kpage.core.paging.common.dbDispatcher
import io.github.wladyslawpopov.kpage.core.paging.data.cache.CacheJson
import io.github.wladyslawpopov.kpage.core.paging.data.cache.LiveCacheKeys
import io.github.wladyslawpopov.kpage.core.paging.data.cache.evictOldLists
import io.github.wladyslawpopov.kpage.core.paging.data.paginator.PagerPayload
import io.github.wladyslawpopov.kpage.core.paging.data.paginator.Paginator
import io.github.wladyslawpopov.kpage.core.paging.data.paginator.PaginatorConfig
import io.github.wladyslawpopov.kpage.core.paging.data.state.LoadState
import io.github.wladyslawpopov.kpage.core.paging.data.state.PagedList
import io.github.wladyslawpopov.kpage.core.paging.data.state.PagingState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlin.coroutines.cancellation.CancellationException


/**
 * A robust, Offline-First paginator for Kotlin Multiplatform.
 * It uses SQLDelight to cache pages locally, ensuring data availability
 * and smooth scrolling even on poor network connections.
 *
 * The cache is addressed by server position: an item lives at `page * pageSize + index`,
 * so a page that shifted on the server never leaves holes in the cached list.
 *
 * The paginator keeps a window of `2 * prefetchDistance + 1` pages around the anchor (the page of
 * the first visible item) loaded, plus `extraPagesInDb` spare pages in the database.
 *
 * All state belongs to one handler coroutine that processes events one by one: UI calls, item
 * edits and page responses. Page loads only call [getPage] and report the result.
 *
 * @param T The type of items being paginated.
 * @param serializer The Serialization serializer for type [T].
 * @param queryKey The name of the list in the local cache (it is never sent to the server), for
 * example built from the query and its filters. The same list may be open on several screens at once:
 * each extra live paginator for the same key keeps its pages separately.
 * @param config Configuration parameters like page size and prefetch distance.
 * @param getPage A suspend function to fetch a page of data from the network. It receives
 * `page + config.initialPageKey`. For a page past the end of the list it should return an empty
 * `objects` list with the actual `totalCount`: while the total count is unknown, a refresh also
 * requests the pages after the anchor.
 * @param idExtractor A lambda to extract a unique string ID from an item.
 * @param db The SQLDelight database instance for caching.
 * @param scope The scope of whoever creates the paginator (a component, a ViewModel, a presenter):
 * the paginator lives exactly as long as it and stops when it is cancelled. Only its job is used;
 * the work runs on [dispatcher]. Call [close] only to stop this paginator earlier, for example when
 * it is replaced after the query changed.
 * @param dispatcher Runs the paginator's work: events, JSON decoding and [getPage] calls.
 * @param databaseDispatcher Runs database work. Paginators of one database should share it: on JVM
 * the default one runs a single operation at a time, which the JDBC drivers need.
 */
class StablePaginator<T : Any>(
    private val serializer: KSerializer<T>,
    queryKey: String,
    private val config: PaginatorConfig = PaginatorConfig(),
    val getPage: suspend (Int) -> PagerPayload<T>,
    private val idExtractor: (T) -> String,
    private val db: PagingDatabase,
    scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val databaseDispatcher: CoroutineDispatcher = dbDispatcher(),
) : Paginator<T> {

    private val itemQueries = db.itemRawQueries
    private val entryQueries = db.listingEntryQueries
    private val metaQueries = db.pageMetadataQueries
    private val queryMetaQueries = db.queryMetaQueries

    // Items of different types may share an id, so the serial name is part of every key.
    private val itemType = serializer.descriptor.serialName
    private val pageSize = config.pageSize

    private class LoadedPage<T>(val payload: PagerPayload<T>, val startTick: Long)

    // A refresh in progress: the requested pages are collected in memory and committed together.
    private class Refresh<T>(val pages: Set<Int>, val overCache: Boolean) {
        val collected = mutableMapOf<Int, LoadedPage<T>>()
        val failures = mutableMapOf<Int, Throwable>()
    }

    // openTail: the total count is unknown and the last cached page ends exactly at the known length
    // with more after it, so the page right after the known length may be requested.
    private class CacheInfo(val loadedPages: Set<Int>, val totalCount: Int, val openTail: Boolean)

    private sealed interface Event {
        class Visible(val pages: IntRange) : Event
        data object Refresh : Event
        class Run(val block: suspend () -> Unit) : Event
        class Loaded<T>(
            val page: Int,
            val generation: Long,
            val load: Job,
            val result: Result<LoadedPage<T>>
        ) : Event
    }

    // Everything below is read and changed only by the handler coroutine.
    // The visible pages: from the page of the first visible item to the page of the last one.
    private var anchor = 0
    private var anchorEnd = 0
    private var generation = 0L
    private var activeRefresh: Refresh<T>? = null
    private val jobs = mutableMapOf<Int, Job>()
    private var error: Throwable? = null
    private var tick = 0L
    private val localEdits = mutableMapOf<String, Long>()

    init {
        require(scope.isActive) { "The scope of the paginator is already cancelled" }
    }

    // The cache key: the queryKey itself, or a key of its own if another live paginator already
    // shows the same list (see LiveCacheKeys). Released when the paginator stops. A name of its own,
    // so the queryKey parameter cannot shadow it in property initializers.
    private val cacheKey: String = LiveCacheKeys.acquire(db, queryKey)

    private val job = SupervisorJob(scope.coroutineContext[Job]).also { job ->
        job.invokeOnCompletion { LiveCacheKeys.release(db, cacheKey) }
    }
    private val paginatorScope = CoroutineScope(job + dispatcher)
    private val events = Channel<Event>(Channel.UNLIMITED)

    // Completed once this list is registered in the cache and old lists are evicted.
    private val initialized = CompletableDeferred<Unit>()

    private val _state = MutableStateFlow(PagingState.initial<T>())

    /**
     * The paginated list, its total count and the load state. Items and total count come from one
     * database snapshot and are always consistent; the load state is updated synchronously.
     */
    override val state: StateFlow<PagingState<T>> = _state.asStateFlow()

    internal suspend fun awaitInitialized() = initialized.await()

    private fun send(event: Event): Boolean {
        return job.isActive && events.trySend(event).isSuccess
    }

    // Runs a block in the handler and waits until it has finished; returns at once after close().
    private suspend fun runAndAwait(block: suspend () -> Unit) {
        val done = CompletableDeferred<Unit>()
        val onClose = job.invokeOnCompletion { done.complete(Unit) }
        try {
            val accepted = send(
                Event.Run {
                    try {
                        block()
                    } finally {
                        done.complete(Unit)
                    }
                }
            )
            if (accepted) done.await()
        } finally {
            onClose.dispose()
        }
    }

    // Called inside a transaction: marks the list as the most recently used and evicts the oldest ones.
    private fun registerQuery() {
        val size = pageSize.toLong()
        queryMetaQueries.insertIfAbsent(cacheKey, size)
        val meta = queryMetaQueries.selectByKey(cacheKey).executeAsOne()
        if (meta.pageSize != size) {
            // Positions of another page size are meaningless here.
            entryQueries.deleteAllEntriesByQuery(cacheKey)
            metaQueries.deleteAllPageMetadataByQuery(cacheKey)
            queryMetaQueries.setPageSize(size, cacheKey)
        }
        queryMetaQueries.touch(cacheKey)
        db.evictOldLists()
    }

    private val jsonSerializer = CacheJson

    // In-memory cache to store parsed objects and avoid redundant JSON deserialization
    private var parsedCache = emptyMap<String, T>()

    private class Snapshot<T : Any>(val totalCount: Int, val items: PagedList<T>)

    // One database query yields the total count together with every cached position, so the two
    // can never disagree. An item that cannot be decoded is a placeholder on its own position.
    private val snapshots: Flow<Snapshot<T>> = flow {
        initialized.await()
        emitAll(queryMetaQueries.getSnapshot(cacheKey).asFlow().mapToList(databaseDispatcher))
    }
        .distinctUntilChanged()
        .map { rows ->
            currentCoroutineContext().ensureActive()

            val totalCount = rows.firstOrNull()?.totalCount?.toInt() ?: 0
            val currentCache = parsedCache
            val newCache = HashMap<String, T>()
            val loaded = HashMap<Int, T>()
            rows.forEach { row ->
                val position = row.position?.toInt() ?: return@forEach
                val json = row.json ?: return@forEach
                if (position !in 0..<totalCount) return@forEach
                val item = currentCache[json] ?: try {
                    jsonSerializer.decodeFromString(serializer, json)
                } catch (e: Throwable) {
                    if (e is CancellationException) throw e
                    null
                }
                if (item != null) {
                    newCache[json] = item
                    loaded[position] = item
                }
            }
            parsedCache = newCache
            Snapshot(totalCount, PagedList(totalCount, loaded))
        }
        .flowOn(dispatcher)

    // Declared after every property it uses: the coroutines start immediately.
    init {
        paginatorScope.launch {
            try {
                withContext(databaseDispatcher) {
                    db.transaction { registerQuery() }
                }
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                error = e
            } finally {
                initialized.complete(Unit)
            }
            // The one renewal the paginator starts by itself: the window at the top. What the cache has is
            // shown at once and replaced when the pages arrive; after this only the app asks for a refresh.
            guarded {
                if (error == null) startRefresh()
            }
            publishLoadState()
            for (event in events) {
                currentCoroutineContext().ensureActive()
                guarded { handle(event) }
                if (jobs.isEmpty() && activeRefresh == null) localEdits.clear()
                publishLoadState()
            }
        }
        paginatorScope.launch {
            snapshots.collect { snapshot ->
                _state.update {
                    it.copy(items = snapshot.items, totalCount = snapshot.totalCount, isRestored = true)
                }
            }
        }
    }

    // ---- Events ----

    // Scrolling within the same pages sends nothing. Written by the UI thread only; a lost update
    // just sends one event more.
    private var lastSentPages: IntRange? = null

    override fun onVisibleRangeChanged(visible: IntRange) {
        val pages = pagesOf(visible)
        if (pages == lastSentPages) return
        lastSentPages = pages
        send(Event.Visible(pages))
    }

    private fun pagesOf(visible: IntRange): IntRange {
        val first = maxOf(visible.first, 0) / pageSize
        return first..maxOf(first, visible.last / pageSize)
    }

    override fun refresh() {
        send(Event.Refresh)
    }

    override fun close() {
        events.cancel()
        paginatorScope.cancel()
    }

    private suspend fun handle(event: Event) {
        when (event) {
            is Event.Visible -> {
                if (event.pages.first == anchor && event.pages.last == anchorEnd) return
                anchor = event.pages.first
                anchorEnd = event.pages.last
                followAnchor()
            }

            Event.Refresh -> startRefresh()

            is Event.Run -> event.block()

            is Event.Loaded<*> -> {
                @Suppress("UNCHECKED_CAST")
                onLoaded(event as Event.Loaded<T>)
            }
        }
    }

    // Runs a failure of any event through the same path.
    private suspend fun guarded(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            if (activeRefresh != null) abandonRefresh()
            error = e
        }
    }

    // The anchor moved: a refresh survives only while all its pages are still in the window.
    private suspend fun followAnchor() {
        val current = activeRefresh
        if (current == null) {
            ensureWindow()
        } else if (!window().containsAll(current.pages)) {
            abandonRefresh()
            ensureWindow()
        }
    }

    // ---- Window ----

    // The visible pages plus prefetchDistance pages on each side, so a screen taller than a page is
    // loaded too.
    private fun window(): IntRange {
        val distance = config.prefetchDistance
        val last = (maxOf(anchor, anchorEnd).toLong() + distance).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        return maxOf(0, anchor - distance)..last
    }

    private fun moveAnchor(page: Int) {
        anchor = page
        anchorEnd = page
    }

    private fun IntRange.containsAll(pages: Set<Int>): Boolean = pages.all { it in this }

    private suspend fun readCache(): CacheInfo = withContext(databaseDispatcher) {
        val pages = metaQueries.getLoadedPageNumbers(cacheKey).executeAsList().map { it.toInt() }.toSet()
        val total = queryMetaQueries.selectByKey(cacheKey).executeAsOneOrNull()?.totalCount ?: 0L
        val latest = metaQueries.getLatestPageMetadata(cacheKey).executeAsOneOrNull()
        // With a known total the last page never has more after it at the total; only an unknown one does.
        val openTail = latest != null && latest.hasMore && (latest.pageNumber + 1) * pageSize == total
        CacheInfo(pages, total.toInt(), openTail)
    }

    private suspend fun hasCache(): Boolean = withContext(databaseDispatcher) {
        entryQueries.countEntries(cacheKey).executeAsOne() > 0
    }

    private suspend fun isEnd(): Boolean = withContext(databaseDispatcher) {
        val latest = metaQueries.getLatestPageMetadata(cacheKey).executeAsOneOrNull()
        latest != null && !latest.hasMore
    }

    // After the anchor the pages are requested only within the known total count, plus the page right
    // after it while the end of a list without a total is not found yet.
    // Without any cached page the total count is not known yet, so the whole window is requested.
    private fun pagesToRequest(window: IntRange, totalCount: Int, openTail: Boolean, totalKnown: Boolean): List<Int> =
        window.filter {
            val start = it.toLong() * pageSize
            it == anchor || !totalKnown || start < totalCount || (openTail && start == totalCount.toLong())
        }

    private fun pagesToRequest(window: IntRange, cache: CacheInfo): List<Int> =
        pagesToRequest(window, cache.totalCount, cache.openTail, totalKnown = cache.loadedPages.isNotEmpty())

    // Loads the pages of the window that are missing; loads outside of it are cancelled.
    private suspend fun ensureWindow() {
        val window = window()
        jobs.keys.filter { it !in window }.forEach { jobs.remove(it)?.cancel() }

        val cache = readCache()
        pagesToRequest(window, cache).forEach { page ->
            if (page !in cache.loadedPages && page !in jobs) startLoad(page)
        }
    }

    private fun startLoad(page: Int) {
        error = null
        val requestGeneration = generation
        val startTick = tick
        val load = paginatorScope.launch {
            val self = currentCoroutineContext()[Job]!!
            val result = try {
                Result.success(LoadedPage(getPage(page + config.initialPageKey), startTick))
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                Result.failure(e)
            }
            send(Event.Loaded(page, requestGeneration, self, result))
        }
        jobs[page] = load
    }

    // Refreshes the window around the anchor; the old data stays until every requested page arrived.
    // [knownTotal] is a total count just received from the server, newer than the cached one.
    private suspend fun startRefresh(knownTotal: Int? = null) {
        generation++
        cancelJobs()
        error = null
        val cache = readCache()
        if (cache.loadedPages.isNotEmpty() && anchor.toLong() * pageSize >= cache.totalCount) {
            moveAnchor(lastPageOf(cache.totalCount))
        }
        val pages = if (knownTotal != null) {
            pagesToRequest(window(), knownTotal, openTail = false, totalKnown = true)
        } else {
            pagesToRequest(window(), cache)
        }
        activeRefresh = Refresh(pages.toSet(), overCache = hasCache())
        pages.forEach { startLoad(it) }
    }

    private fun abandonRefresh() {
        generation++
        cancelJobs()
        activeRefresh = null
    }

    private fun cancelJobs() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
    }

    private fun lastPageOf(totalCount: Int): Int = maxOf(0, (totalCount - 1) / pageSize)

    // ---- Loading ----

    private suspend fun onLoaded(event: Event.Loaded<T>) {
        val page = event.page
        // A response of a cancelled, replaced or reset load is obsolete.
        if (event.generation != generation || jobs[page] !== event.load) return
        jobs.remove(page)

        val current = activeRefresh
        if (current == null) {
            val loaded = event.result.getOrElse {
                error = it
                return
            }
            val before = readCache()
            inTransaction { persistPage(page, loaded) }
            loadNewlyAllowed(before)
            return
        }

        event.result
            .onSuccess { current.collected[page] = it }
            .onFailure { current.failures[page] = it }
        // The list got shorter than the anchor: refresh around the new end. Pages past the end may
        // fail on the server, so failures are judged only after every page has answered.
        val received = event.result.getOrNull()?.payload?.exactTotal(page)
        if (received != null && anchor > lastPageOf(received)) {
            moveAnchor(lastPageOf(received))
            startRefresh(knownTotal = received)
            return
        }
        if (current.collected.size + current.failures.size < current.pages.size) return
        // Pages past an exact length returned by another page do not exist: their failures (many
        // servers answer 400/404 there) do not count.
        val exact = current.collected.entries.mapNotNull { (p, loaded) -> loaded.payload.exactTotal(p) }.minOrNull()
        current.failures.filterKeys { exact == null || it.toLong() * pageSize < exact }.values.firstOrNull()?.let {
            abandonRefresh()
            error = it
            return
        }
        inTransaction { commitRefresh(current) }
        activeRefresh = null
        val total = readCache().totalCount
        if (anchor.toLong() * pageSize >= total) moveAnchor(lastPageOf(total))
        ensureWindow()
    }

    // A page can reveal more of the list (a larger total, or an open tail without a total): the window
    // pages it allowed are loaded now, not on the next page change. Pages allowed before are left
    // alone, so a failed one is not retried here.
    private suspend fun loadNewlyAllowed(before: CacheInfo) {
        val after = readCache()
        val window = window()
        val allowedBefore = pagesToRequest(window, before).toSet()
        pagesToRequest(window, after)
            .filter { it !in allowedBefore && it !in after.loadedPages && it !in jobs }
            .forEach { startLoad(it) }
    }

    // Called at the end of every event.
    private suspend fun publishLoadState() {
        val current = activeRefresh
        val failure = error
        val loadState = try {
            when {
                failure != null -> LoadState.ERROR(failure)
                current != null -> if (current.overCache) LoadState.REFRESH else LoadState.INITIAL
                jobs.isNotEmpty() -> if (hasCache()) LoadState.NEXT else LoadState.INITIAL
                isEnd() -> LoadState.END
                else -> LoadState.IDLE
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            LoadState.ERROR(e)
        }
        _state.update { it.copy(loadState = loadState) }
    }

    // ---- Database transactions (called by the handler) ----

    private suspend fun inTransaction(block: () -> Unit) {
        withContext(databaseDispatcher) {
            currentCoroutineContext().ensureActive()
            db.transaction { block() }
        }
    }

    // Replaces the page's positions and writes its items. An item edited after the request started
    // keeps its local JSON, so an older response cannot undo the edit.
    private fun writePage(page: Int, loaded: LoadedPage<T>) {
        val payload = loaded.payload
        val start = page.toLong() * pageSize
        entryQueries.deleteEntriesInRange(cacheKey, start, start + pageSize)
        payload.objects.forEachIndexed { index, item ->
            val id = idExtractor(item)
            val json = jsonSerializer.encodeToString(serializer, item)
            val edited = (localEdits[id] ?: Long.MIN_VALUE) > loaded.startTick
            if (edited) itemQueries.insertItemIfAbsent(itemType, id, json) else itemQueries.upsertItem(itemType, id, json)
            entryQueries.upsertEntry(cacheKey, start + index, itemType, id)
        }
        metaQueries.upsertPageMetadata(cacheKey, page.toLong(), payload.hasMoreAfter(page))
    }

    private fun PagerPayload<T>.hasMoreAfter(page: Int): Boolean {
        val end = page.toLong() * pageSize + objects.size
        val total = totalCount
        return if (total != null) isMore || total > end else isMore && objects.size >= pageSize
    }

    // The total count this page proves exactly: the server's one, or the end of the last page.
    private fun PagerPayload<T>.exactTotal(page: Int): Int? =
        totalCount ?: (page * pageSize + objects.size).takeIf { !hasMoreAfter(page) }

    // Without a server total, a page with more after it gives only a lower bound: a refresh of an
    // upper page must not cut the list the user has already scrolled.
    private fun PagerPayload<T>.lengthWith(page: Int, knownTotal: Int): Int =
        exactTotal(page) ?: maxOf(knownTotal, page * pageSize + objects.size)

    private fun persistPage(page: Int, loaded: LoadedPage<T>) {
        queryMetaQueries.insertIfAbsent(cacheKey, pageSize.toLong())
        writePage(page, loaded)
        // A list in use stays the most recently used one, so it is never the first to be evicted.
        queryMetaQueries.touch(cacheKey)
        val known = queryMetaQueries.selectByKey(cacheKey).executeAsOneOrNull()?.totalCount?.toInt() ?: 0
        val totalCount = loaded.payload.lengthWith(page, known)
        queryMetaQueries.setTotalCount(totalCount.toLong(), cacheKey)
        trimToTotal(totalCount)
        prunePages()
        itemQueries.deleteUnreferenced()
    }

    // Replaces the whole list with the collected pages: pages outside of them are stale and dropped.
    private fun commitRefresh(current: Refresh<T>) {
        entryQueries.deleteAllEntriesByQuery(cacheKey)
        metaQueries.deleteAllPageMetadataByQuery(cacheKey)
        queryMetaQueries.insertIfAbsent(cacheKey, pageSize.toLong())
        queryMetaQueries.touch(cacheKey)
        // The oldest request decides which local edits are protected.
        val startTick = current.collected.values.minOf { it.startTick }
        current.collected.keys.sorted().forEach { page ->
            writePage(page, LoadedPage(current.collected.getValue(page).payload, startTick))
        }
        // The pages outside the window were dropped, so the last collected page alone sets the length.
        val lastPage = current.collected.keys.max()
        val totalCount = current.collected.getValue(lastPage).payload.lengthWith(lastPage, knownTotal = 0)
        queryMetaQueries.setTotalCount(totalCount.toLong(), cacheKey)
        trimToTotal(totalCount)
        itemQueries.deleteUnreferenced()
    }

    // Positions at or past the total no longer exist. Page 0 stays, so an empty list remains "loaded".
    private fun trimToTotal(totalCount: Int) {
        entryQueries.deleteEntriesFrom(cacheKey, totalCount.toLong())
        val firstPageBeyond = maxOf(1, (totalCount + pageSize - 1) / pageSize)
        metaQueries.deletePageMetadataFrom(cacheKey, firstPageBeyond.toLong())
    }

    // Keeps the window and the nearest `extraPagesInDb` pages around it; the rest is deleted.
    private fun prunePages() {
        val window = window()
        val existing = (
                entryQueries.getExistingPageNumbers(pageSize.toLong(), cacheKey).executeAsList() +
                        metaQueries.getLoadedPageNumbers(cacheKey).executeAsList()
                ).map { it.toInt() }.toSet()
        val others = existing.filter { it !in window }
            .sortedWith(
                compareBy(
                    { if (it < window.first) window.first - it else it - window.last },
                    { it }
                )
            )
        others.drop(config.extraPagesInDb).forEach { page ->
            val start = page.toLong() * pageSize
            entryQueries.deleteEntriesInRange(cacheKey, start, start + pageSize)
            metaQueries.deletePageMetadataByNumber(cacheKey, page.toLong())
        }
    }

    // ---- Item access and edits ----

    /**
     * Retrieves a single item directly from the local database by its ID.
     */
    override suspend fun getItemById(id: String): T? = withContext(databaseDispatcher) {
        itemQueries.selectById(itemType, id).executeAsOneOrNull()?.let { decode(it.json) }
    }

    /**
     * Applies [transform] to the cached item and stores the result in one step, so it always gets the
     * current version. The change reflects at every position where the item occurs, in every list of
     * this item type. Responses of this paginator's loads that started before the edit do not
     * overwrite it; a load of another list started before the edit may bring the old version back
     * until its next refresh.
     * Returns the new item, or null if the item is not cached. An exception of [transform] is thrown
     * to the caller and does not affect the paginator.
     */
    override suspend fun updateItem(id: String, transform: (T) -> T): T? {
        var updated: T? = null
        var failure: Throwable? = null
        runAndAwait {
            try {
                updated = withContext(databaseDispatcher) {
                    db.transactionWithResult {
                        val raw = itemQueries.selectById(itemType, id).executeAsOneOrNull()
                        val current = raw?.let { decode(it.json) }
                        current?.let(transform)?.also { next ->
                            itemQueries.updateJson(jsonSerializer.encodeToString(serializer, next), itemType, id)
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                failure = e
            }
            if (updated != null) localEdits[id] = ++tick
        }
        failure?.let { throw it }
        return updated
    }

    private fun decode(json: String): T? = try {
        jsonSerializer.decodeFromString(serializer, json)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        null
    }
}
