package io.github.wladyslawpopov.kpager.core.paging

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.github.wladyslawpopov.kpager.cache.PagingDataBase
import io.github.wladyslawpopov.kpager.core.paging.common.dbDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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

    // Tracks which pages are currently being fetched to prevent duplicate network requests
    private val pagesCurrentlyLoading = mutableSetOf<Int>()

    private val job = SupervisorJob()
    private val paginatorScope = CoroutineScope(job + Dispatchers.Default)
    private var refreshJob: Job? = null

    private val itemQueries = db.itemRawQueries
    private val entryQueries = db.listingEntryQueries
    private val metaQueries = db.pageMetadataQueries

    private val jsonSerializer = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
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

                val parsedItems = dbItems.mapNotNull { row ->
                    val item = if (row.json == "{}") null else {
                        try {
                            currentCache[row.json] ?: jsonSerializer.decodeFromString(serializer, row.json)
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            throw e
                        }
                    }

                    if (item != null) {
                        newCache[row.json] = item
                        row.pageNumber to item
                    } else null
                }

                parsedCache = newCache

                if (parsedItems.isEmpty()) return@map emptyMap()

                val minPage = parsedItems.minOf { it.first }.toInt()

                // Calculate the theoretical starting index for the earliest loaded page
                val startOffset = minPage * config.pageSize

                val resultMap = LinkedHashMap<Int, T?>(parsedItems.size)

                // We recalculate the global index sequentially (startOffset + index)
                // instead of relying purely on the raw database sequence.
                // If an item was deleted from the database, the original sequence
                // would have "holes" (missing indices). Re-indexing here ensures
                // the UI list remains strictly continuous without empty gaps.
                parsedItems.forEachIndexed { index, pair ->
                    val globalIndex = startOffset + index
                    resultMap[globalIndex] = pair.second
                }

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

    /**
     * Triggers the fetching of a specific page.
     * Usually called by the UI when the user scrolls near the end of the loaded list.
     */
    override fun onPrefetch(pageToLoad: Int) {
        if (_loadState.value is LoadState.INITIAL) return
        loadPagesAround(pageToLoad)
    }

    /**
     * Retrieves a single item directly from the local database by its ID.
     */
    override suspend fun getItemById(id: String): T? {
        return withContext(dbDispatcher()){
            val currentRaw = itemQueries.selectById(id).executeAsOneOrNull()

            return@withContext if (currentRaw != null) {
                jsonSerializer.decodeFromString(serializer, currentRaw.json)
            } else {
                null
            }
        }
    }

    /**
     * Resets the paginator state and reloads data starting from the specified global index.
     * Useful for pull-to-refresh or restoring scroll position.
     */
    override fun reset(index: Int) {
        paginatorScope.launch {
            mutex.withLock {
                pagesCurrentlyLoading.clear()
                setLoadState(LoadState.INITIAL)
            }

            val pageToLoad = index / config.pageSize
            val targetPage =
                if (pageToLoad > config.initialPageKey) pageToLoad else config.initialPageKey

            loadPagesAround(targetPage, forceUpdate = true)
        }
    }

    /**
     * Cancels all ongoing pagination coroutines. Should be called when the ViewModel/Component is destroyed.
     */
    override fun close() {
        refreshJob?.cancel()
        paginatorScope.cancel()
    }

    private fun loadPagesAround(targetPage: Int, forceUpdate: Boolean = false) {
        val pageToLoad = maxOf(targetPage, config.initialPageKey).toLong()

        val pagesToRequest = mutableListOf<Long>()
        pagesToRequest.add(pageToLoad)

        val currentTotal = _totalCount.value
        val loadedItemsCount = (pageToLoad + 1) * config.pageSize

        if (currentTotal == 0 || loadedItemsCount < currentTotal) {
            if (config.prefetchDistance > 0) {
                pagesToRequest.add(pageToLoad + 1)
            }
        }

        pagesToRequest.distinct().forEach { page ->
            paginatorScope.launch {
                performPageLoad(page.toInt(), forceUpdate)
            }
        }
    }

    private suspend fun performPageLoad(pageNumber: Int, forceUpdate: Boolean = false) {
        if (pagesCurrentlyLoading.contains(pageNumber)) return

        mutex.withLock {
            if (pagesCurrentlyLoading.contains(pageNumber)) return
            pagesCurrentlyLoading.add(pageNumber)

            if (_loadState.value !is LoadState.INITIAL && !forceUpdate) {
                setLoadState(LoadState.NEXT)
            }
        }

        try {
            val payload = fetchPage(pageNumber)

            val items = payload.objects
            val total = payload.totalCount
            val hasMore = payload.isMore || (total > (pageNumber * config.pageSize) + items.size)
            dbMutex.withLock {
                withContext(dbDispatcher()) {
                    db.transaction {
                        processItemsRaw(items)
                        entryQueries.deleteEntriesByPageNumber(queryKey, pageNumber.toLong())
                        processListingEntries(pageNumber.toLong(), items, config.pageSize)
                        processPageMetadata(pageNumber.toLong(), total.toLong(), hasMore)
                        pruneOldPages(pageNumber)
                    }
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            setLoadState(LoadState.ERROR(e.message ?: "Unknown error"))
        } finally {
            mutex.withLock {
                pagesCurrentlyLoading.remove(pageNumber)

                if (pagesCurrentlyLoading.isEmpty() && _loadState.value !is LoadState.ERROR) {
                    val isEnd = withContext(dbDispatcher()) {
                        val latestPage =
                            metaQueries.getLatestPageMetadata(queryKey).executeAsOneOrNull()
                        latestPage != null && latestPage.nextPageKey == null
                    }
                    setLoadState(if (isEnd) LoadState.END else LoadState.IDLE)
                }
            }
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
    private fun pruneOldPages(currentPageNumber: Int) {
        val existingPages = entryQueries.getExistingPageNumbers(queryKey).executeAsList()
        val buffer = 2
        if (existingPages.size <= config.maxPagesInDb + buffer) return

        val pagesToDelete = existingPages.sortedByDescending { pageNum ->
            abs(currentPageNumber - pageNum.toInt())
        }.take(existingPages.size - (config.maxPagesInDb + buffer))

        pagesToDelete.forEach { pageNumToDelete ->
            entryQueries.deleteEntriesByPageNumber(queryKey, pageNumToDelete)
            metaQueries.deletePageMetadataByNumber(queryKey, pageNumToDelete)
        }
    }

    private suspend fun fetchPage(pageNumber: Int): PagerPayload<T> {
        val payload = getPage(pageNumber)
        _totalCount.value = payload.totalCount
        return payload
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
