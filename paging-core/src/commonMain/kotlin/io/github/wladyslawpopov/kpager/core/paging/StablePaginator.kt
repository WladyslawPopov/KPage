package io.github.wladyslawpopov.kpager.core.paging

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.github.wladyslawpopov.kpager.cache.PagingDataBase
import io.github.wladyslawpopov.kpager.core.paging.common.dbDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import io.github.wladyslawpopov.kpager.core.paging.data.LoadState
import io.github.wladyslawpopov.kpager.core.paging.data.Paginator
import io.github.wladyslawpopov.kpager.core.paging.data.PaginatorConfig
import io.github.wladyslawpopov.kpager.core.paging.data.PagerPayload
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs

/**
 * A robust, Offline-First paginator for Kotlin Multiplatform.
 * It uses SQLDelight to cache pages locally, ensuring data availability
 * and smooth scrolling even on poor network connections.
 *
 * @param T The type of items being paginated.
 * @param serializer The kotlinx.serialization serializer for type [T].
 * @param queryKey A unique identifier for the current pagination query/list.
 * @param config Configuration parameters like page size and prefetch distance.
 * @param getPage A suspend function to fetch a page of data from the network.
 * @param idExtractor A lambda to extract a unique string ID from an item.
 * @param db The SQLDelight database instance for caching.
 */
class StablePaginator<T : Any>(
    private val serializer: KSerializer<T>,
    private val queryKey: String,
    private val config: PaginatorConfig = PaginatorConfig(),
    val getPage: suspend (Int) -> PagerPayload<T>,
    private val idExtractor: (T) -> String,
    private val db: PagingDataBase
) : Paginator<T> {

    private val _loadState = MutableStateFlow<LoadState>(LoadState.IDLE)
    override val loadState = _loadState.asStateFlow()

    private val _totalCount = MutableStateFlow(0)
    override val totalCount = _totalCount.asStateFlow()

    private val mutex = Mutex()
    private val dbMutex = Mutex()

    // Accessed only under mutex. A reset invalidates both requests and their results.
    private val pageJobs = mutableMapOf<Int, Job>()
    private var generation = 0L
    private var replaceCacheOnNextLoad = false
    private val refreshPages = mutableMapOf<Int, PagerPayload<T>>()
    private var retainedPages = config.initialPageKey..config.initialPageKey

    private val job = SupervisorJob()
    private val paginatorScope = CoroutineScope(job + Dispatchers.Default)
    private val commands = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        // Process UI commands in submission order, including position changes and resets.
        paginatorScope.launch {
            for (command in commands) {
                mutex.withLock {
                    try {
                        currentCoroutineContext().ensureActive()
                        command()
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        setLoadState(LoadState.ERROR(e))
                    }
                }
            }
        }
    }

    private fun submit(command: suspend () -> Unit) {
        if (job.isActive) commands.trySend(command)
    }

    private val itemQueries = db.itemRawQueries
    private val entryQueries = db.listingEntryQueries
    private val metaQueries = db.pageMetadataQueries

    private val jsonSerializer = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
    }

    // In-memory cache to store parsed objects and avoid redundant JSON deserialization
    private var parsedCache = emptyMap<String, T>()

    /**
     * A reactive stream of the paginated items mapped by their global index.
     * Automatically updates whenever the underlying database changes.
     */
    override val itemsMap: StateFlow<Map<Int, T?>> =
        entryQueries.getItemsWithOrderForQuery(queryKey)
            .asFlow()
            .mapToList(dbDispatcher())
            .flowOn(dbDispatcher())
            .distinctUntilChanged()
            .map { dbItems ->
                currentCoroutineContext().ensureActive()

                if (dbItems.isEmpty()) {
                    parsedCache = emptyMap()
                    return@map emptyMap()
                }

                val currentCache = parsedCache
                val newCache = mutableMapOf<String, T>()
                val resultMap = LinkedHashMap<Int, T?>(dbItems.size)

                dbItems.groupBy { it.pageNumber }.forEach { (pageNumber, itemsOfPage) ->
                    val pageStartOffset = pageNumber.toInt() * config.pageSize
                    
                    var validItemIndex = 0
                    itemsOfPage.forEach { row ->
                        val item = if (row.json == "{}") null else {
                            try {
                                currentCache[row.json] ?: jsonSerializer.decodeFromString(serializer, row.json)
                            } catch (e: Throwable) {
                                if (e is CancellationException) throw e
                                null
                            }
                        }

                        if (item != null) {
                            newCache[row.json] = item
                            resultMap[pageStartOffset + validItemIndex] = item
                            validItemIndex++
                        }
                    }
                }

                parsedCache = newCache
                resultMap
            }
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = paginatorScope,
                started = SharingStarted.Lazily,
                initialValue = emptyMap()
            )

    private fun setLoadState(newState: LoadState) {
        if (_loadState.value != newState) {
            _loadState.value = newState
        }
    }

    /** Updates the scroll anchor and loads missing pages around it, in either scroll direction. */
    override fun onVisiblePageChanged(pageIndex: Int) = submit {
        val target = maxOf(pageIndex, config.initialPageKey)
        setVisiblePage(target)
        loadPagesAround(target)
        finishLoadingIfIdle()
    }

    // Cancel obsolete loads before scheduling replacements. Their cleanup is guarded by job identity.
    private fun setVisiblePage(page: Int) {
        val distance = config.prefetchDistance.coerceAtLeast(0)
        val first = maxOf(config.initialPageKey, page - distance)
        val last = (page.toLong() + distance).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        retainedPages = first..last
        val obsolete = pageJobs.filterKeys { it !in retainedPages }
        if (replaceCacheOnNextLoad && (obsolete.isNotEmpty() || refreshPages.keys.any { it !in retainedPages })) {
            // Leaving the refresh window abandons the batch and keeps the previous cache intact.
            generation++
            pageJobs.values.forEach { it.cancel() }
            pageJobs.clear()
            refreshPages.clear()
            replaceCacheOnNextLoad = false
            return
        }
        obsolete.forEach { (number, load) ->
            pageJobs.remove(number)
            load.cancel()
        }
    }

    /**
     * Retrieves a single item directly from the local database by its ID.
     */
    override suspend fun getItemById(id: String): T? {
        return withContext(dbDispatcher()) {
            val currentRaw = itemQueries.selectById(id).executeAsOneOrNull()

            return@withContext if (currentRaw != null) {
                try {
                    jsonSerializer.decodeFromString(serializer, currentRaw.json)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    null
                }
            } else {
                null
            }
        }
    }

    /**
     * Resets the paginator state and reloads data starting from the specified global index.
     * Useful for pull-to-refresh or restoring scroll position. Refresh responses replace
     * cached pages atomically when the requested window is ready; failures keep the offline cache.
     */
    override fun reset(index: Int) = submit {
        generation++
        replaceCacheOnNextLoad = true
        refreshPages.clear()
        pageJobs.values.forEach { it.cancel() }
        pageJobs.clear()
        setLoadState(LoadState.INITIAL)
        val targetPage = maxOf(index / config.pageSize, config.initialPageKey)
        setVisiblePage(targetPage)
        loadPagesAround(targetPage, forceUpdate = true)
    }

    /**
     * Cancels all ongoing pagination coroutines. Should be called when the ViewModel/Component is destroyed.
     */
    override fun close() {
        commands.cancel()
        paginatorScope.cancel()
    }

    // Called under mutex; register jobs before starting them so concurrent prefetches cannot duplicate them.
    private suspend fun loadPagesAround(targetPage: Int, forceUpdate: Boolean = false) {
        val pageToLoad = maxOf(targetPage, config.initialPageKey)
        val currentTotal = _totalCount.value
        val pagesToRequest = retainedPages.filter { page ->
            page == pageToLoad || currentTotal == 0 || page.toLong() * config.pageSize < currentTotal
        }

        val cachedPages = if (forceUpdate) emptySet() else withContext(dbDispatcher()) {
            pagesToRequest.filter { metaQueries.getPageMetadata(queryKey, it.toLong()).executeAsOneOrNull() != null }.toSet()
        }
        val requestGeneration = generation
        pagesToRequest.forEach { page ->
            if (pageJobs.containsKey(page) || page in cachedPages || page in refreshPages) return@forEach
            if (_loadState.value !is LoadState.INITIAL && !forceUpdate) {
                setLoadState(LoadState.NEXT)
            }
            val pageJob = paginatorScope.launch(start = CoroutineStart.LAZY) {
                performPageLoad(page, requestGeneration)
            }
            pageJobs[page] = pageJob
            pageJob.start()
        }
    }

    private suspend fun performPageLoad(pageNumber: Int, requestGeneration: Long) {
        val requestJob = currentCoroutineContext()[Job]
        try {
            val payload = getPage(pageNumber)
            currentCoroutineContext().ensureActive()

            // Serialize reset with responses so obsolete requests cannot overwrite a new generation.
            mutex.withLock {
                currentCoroutineContext().ensureActive()
                if (requestGeneration != generation) return
                if (replaceCacheOnNextLoad) {
                    refreshPages[pageNumber] = payload
                } else {
                    persistPages(mapOf(pageNumber to payload), replaceQuery = false)
                }
            }
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            currentCoroutineContext().ensureActive()
            mutex.withLock {
                if (requestGeneration == generation) {
                    setLoadState(LoadState.ERROR(e))
                }
            }
        } finally {
            // Cancellation must not prevent cleanup, and old jobs must not remove new jobs for the same page.
            withContext(NonCancellable) {
                mutex.withLock {
                    if (requestGeneration == generation && pageJobs[pageNumber] === requestJob) {
                        pageJobs.remove(pageNumber)
                        finishLoadingIfIdle()
                    }
                }
            }
        }
    }

    // Called under mutex, including when a scroll cancels all loads and the new position is cached.
    private suspend fun finishLoadingIfIdle() {
        if (!job.isActive || pageJobs.isNotEmpty() || _loadState.value is LoadState.ERROR) return
        try {
            if (replaceCacheOnNextLoad && refreshPages.isNotEmpty()) {
                persistPages(refreshPages.toMap(), replaceQuery = true)
                replaceCacheOnNextLoad = false
                refreshPages.clear()
            }
            val isEnd = withContext(dbDispatcher()) {
                val latestPage = metaQueries.getLatestPageMetadata(queryKey).executeAsOneOrNull()
                latestPage != null && latestPage.nextPageKey == null
            }
            setLoadState(if (isEnd) LoadState.END else LoadState.IDLE)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            setLoadState(LoadState.ERROR(e))
        }
    }

    // Called under mutex. Publish the complete refresh window in one database transaction.
    private suspend fun persistPages(pages: Map<Int, PagerPayload<T>>, replaceQuery: Boolean) {
        dbMutex.withLock {
            withContext(dbDispatcher()) {
                currentCoroutineContext().ensureActive()
                db.transaction {
                    if (replaceQuery) {
                        entryQueries.deleteAllEntriesByQuery(queryKey)
                        metaQueries.deleteAllPageMetadataByQuery(queryKey)
                    }
                    pages.entries.sortedBy { it.key }.forEach { (pageNumber, payload) ->
                        val items = payload.objects
                        val total = payload.totalCount
                        val hasMore = payload.isMore || total > pageNumber.toLong() * config.pageSize + items.size
                        processItemsRaw(items)
                        entryQueries.deleteEntriesByPageNumber(queryKey, pageNumber.toLong())
                        processListingEntries(pageNumber.toLong(), items, config.pageSize)
                        processPageMetadata(pageNumber.toLong(), total.toLong(), hasMore)
                    }
                    pruneOldPages()
                }
            }
            _totalCount.value = pages.values.last().totalCount
        }
    }

    private fun processItemsRaw(items: List<T>) {
        val now = nowAsEpochSeconds()
        items.forEach { item ->
            val itemId = idExtractor(item)
            val itemJson = jsonSerializer.encodeToString(serializer, item)
            itemQueries.updateItemIfExists(itemJson, now, itemId, itemJson, now)
            itemQueries.insertItemIfAbsent(itemId, itemJson, now)
        }
    }

    private fun processListingEntries(pageNumber: Long, items: List<T>, itemsPageSize: Int) {
        items.forEachIndexed { index, item ->
            val itemId = idExtractor(item)
            val itemOrder = pageNumber * itemsPageSize + index
            entryQueries.insertEntryIfAbsent(queryKey, itemId, itemOrder, pageNumber)
        }
    }

    private fun processPageMetadata(pageNumber: Long, totalCount: Long, hasMore: Boolean) {
        val nextPageKey = if (hasMore) (pageNumber + 1).toString() else null
        metaQueries.updatePageMetadataIfExists(nextPageKey, totalCount, queryKey, pageNumber)
        metaQueries.insertPageMetadataIfAbsent(queryKey, pageNumber, nextPageKey, totalCount)
    }

    // Deletes pages from the database that are outside the allowed maximum limit
    private fun pruneOldPages() {
        val existingPages = entryQueries.getExistingPageNumbers(queryKey).executeAsList()
        val buffer = 2
        if (existingPages.size <= config.maxPagesInDb + buffer) return

        val protectedPages = retainedPages
        val limit = maxOf(config.maxPagesInDb + buffer, protectedPages.last - protectedPages.first + 1)
        val pagesToDelete = existingPages.filter { it.toInt() !in protectedPages }
            .sortedByDescending { pageNum ->
                val page = pageNum.toInt()
                minOf(abs(page.toLong() - protectedPages.first), abs(page.toLong() - protectedPages.last))
            }.take((existingPages.size - limit).coerceAtLeast(0))

        pagesToDelete.forEach { pageNumToDelete ->
            entryQueries.deleteEntriesByPageNumber(queryKey, pageNumToDelete)
            metaQueries.deletePageMetadataByNumber(queryKey, pageNumToDelete)
        }
    }

    /**
     * Optimistically updates an item in the local cache.
     * The changes will immediately reflect in the [itemsMap] flow.
     */
    override suspend fun updateItem(id: String, updatedItem: T) {
        withContext(dbDispatcher()) {
            dbMutex.withLock {
                db.transaction {
                    val itemQueries = db.itemRawQueries
                    val currentRaw = itemQueries.selectById(id).executeAsOneOrNull()

                    if (currentRaw != null) {
                        val newJson = jsonSerializer.encodeToString(serializer, updatedItem)

                        val now = nowAsEpochSeconds()
                        val oldTimestamp = currentRaw.updatedAt

                        val newTimestamp = if (now <= oldTimestamp) oldTimestamp + 1 else now

                        itemQueries.updateItemIfExists(
                            newJson,
                            newTimestamp,
                            id,
                            newJson,
                            newTimestamp
                        )
                    }
                }
            }
        }
    }
}
