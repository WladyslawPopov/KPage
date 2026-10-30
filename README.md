# KPage

[![Maven Central](https://img.shields.io/maven-central/v/io.github.wladyslawpopov/paging-core)](https://central.sonatype.com/artifact/io.github.wladyslawpopov/paging-core)
[![Tests](https://github.com/WladyslawPopov/KPage/actions/workflows/tests.yml/badge.svg)](https://github.com/WladyslawPopov/KPage/actions/workflows/tests.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

Pagination library for Kotlin Multiplatform with a local SQLDelight cache and a Compose Multiplatform list component.

Pages are written to a local database first, and the UI always reads from that database. When the app restarts, the list shows cached data immediately and refreshes it from the network in the background. If the network fails, the cached items stay on screen.

Targets: **Android**, **iOS** (`iosArm64`, `iosSimulatorArm64`), **JVM / Desktop**.

## Features

- Cache-first loading: cached pages are shown first, then replaced with fresh data from the network.
- The paginator decides what to load. The UI only reports which page is visible.
- Neighbouring pages are prefetched; requests for pages the user has scrolled away from are cancelled.
- Old pages are removed from the database so the cache does not grow without limit.
- `updateItem()` changes one item in the cache, and the list updates without reloading the page.
- `PagingLayout` for Compose: lists, grids, reversed lists for chats, and lists with headers or separators.
- Covered by unit and Compose UI tests that run in GitHub Actions.

## Installation

The library is published to Maven Central. Add it to `commonMain`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.wladyslawpopov:paging-core:1.0.6")
        }
    }
}
```

Make sure `mavenCentral()` is in your repositories. Gradle picks the right platform artifact (`-android`, `-jvm`, `-iosarm64`, `-iossimulatorarm64`) automatically.

The library is built with Kotlin 2.4, Compose Multiplatform 1.10, SQLDelight 2.3 and Koin 4.2.

## Database setup

The paginator needs a `PagingDataBase` instance. One database can be shared by all paginators in the app.

### With Koin

The library ships a ready Koin module that creates the platform driver:

```kotlin
import io.github.wladyslawpopov.kpager.core.paging.di.pagingDriverModule

startKoin {
    androidContext(this@App) // Android only: the driver needs a Context
    modules(pagingDriverModule, appModule)
}
```

Then inject it anywhere:

```kotlin
val appModule = module {
    factory { UsersViewModel(api = get(), db = get()) }
}
```

The cache is stored in a file on every platform. On JVM the default location is `~/.kpage/PagingDataBase.db`. If several desktop apps use the library on one machine, give each app its own file:

```kotlin
System.setProperty("kpage.db.path", "${System.getProperty("user.home")}/.myapp/cache.db")
```

Set it before Koin creates the database.

### Without Koin

Create the driver yourself and pass it to `PagingDataBase`:

```kotlin
import io.github.wladyslawpopov.kpager.cache.PagingDataBase

// Android
val driver = AndroidSqliteDriver(PagingDataBase.Schema, context, "PagingDataBase")

// iOS
val driver = NativeSqliteDriver(PagingDataBase.Schema, "PagingDataBase")

// JVM
val driver = JdbcSqliteDriver("jdbc:sqlite:paging.db", schema = PagingDataBase.Schema)

val db = PagingDataBase(driver)
```

In this case add the SQLDelight driver for each platform to your project.

## Quick start

### 1. Model and API

The item type must be `@Serializable`, because pages are stored in the database as JSON. Your API call returns a `PagerPayload`:

```kotlin
@Serializable
data class User(
    val id: String,
    val name: String,
    val avatarUrl: String?,
    val isLiked: Boolean = false,
)

class UsersApi(private val client: HttpClient) {
    suspend fun getUsers(page: Int, pageSize: Int): PagerPayload<User> {
        val response: UsersResponse = client.get("users") {
            parameter("page", page)
            parameter("limit", pageSize)
        }.body()

        return PagerPayload(
            totalCount = response.total,  // total number of items on the server
            isMore = response.hasNext,    // whether there is a next page
            objects = response.items,
        )
    }
}
```

### 2. Paginator in a ViewModel

```kotlin
class UsersViewModel(
    private val api: UsersApi,
    db: PagingDataBase,
) : ViewModel() {

    private val config = PaginatorConfig(pageSize = 30)

    private val paginator = StablePaginator(
        serializer = User.serializer(),
        queryKey = "users",               // unique key for this list in the cache
        config = config,
        getPage = { page -> api.getUsers(page, config.pageSize) },
        idExtractor = { it.id },
        db = db,
    )

    val uiState: StateFlow<UsersUiState> =
        combine(
            paginator.itemsMap,
            paginator.totalCount,
            paginator.loadState,
        ) { items, total, loadState ->
            UsersUiState(
                items = IndexedPageList(items, total),
                totalCount = total,
                loadState = loadState,
            )
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UsersUiState())

    init {
        paginator.reset(index = 0)
    }

    fun onVisiblePageChanged(page: Int) = paginator.onVisiblePageChanged(page)

    fun refresh() = paginator.reset(index = 0)

    override fun onCleared() {
        paginator.close()
    }
}

data class UsersUiState(
    val items: List<User?> = emptyList(),
    val totalCount: Int = 0,
    val loadState: LoadState = LoadState.IDLE,
)
```

### 3. Turning `itemsMap` into a list

`itemsMap` is keyed by the item's position on the server, not by its position in the cache. After old pages are removed, the map can look like `{120 -> a, 121 -> b, ...}` with nothing below 120.

Do not use `itemsMap.values.toList()`: the indices will shift and the list will jump. Instead, wrap the map in a list of the full size, where missing positions return `null` (a placeholder). This adapter does not allocate anything for empty positions:

```kotlin
class IndexedPageList<T>(
    private val items: Map<Int, T?>,
    override val size: Int,
) : AbstractList<T?>() {
    override fun get(index: Int): T? = items[index]
}
```

### 4. Compose screen

```kotlin
@Composable
fun UsersScreen(viewModel: UsersViewModel) {
    val state by viewModel.uiState.collectAsState()

    PagingLayout(
        items = state.items,
        totalCount = state.totalCount,
        pageSize = 30,
        keyExtractor = { _, user -> user.id },
        onVisiblePageChanged = viewModel::onVisiblePageChanged,
        counterBar = { position -> Text("$position / ${state.totalCount}") },
    ) { _, user ->
        if (user != null) {
            UserRow(user)
        } else {
            UserRowPlaceholder()
        }
    }
}
```

`PagingLayout` reports the first visible page through `onVisiblePageChanged`. The paginator loads that page and its neighbours, takes pages from the cache when they are there, and cancels requests that are no longer needed.

## Loading state

`loadState` tells the UI what the paginator is doing:

| State | Meaning |
|---|---|
| `IDLE` | Nothing is loading |
| `INITIAL` | First load or refresh after `reset()` |
| `NEXT` | Loading neighbouring pages while scrolling |
| `END` | The last page has been loaded |
| `ERROR(error)` | A request failed; the original `Throwable` is kept |

On error the cached items are not removed, so the user still sees the list:

```kotlin
when (val load = state.loadState) {
    LoadState.INITIAL -> if (state.totalCount == 0) FullScreenLoader()
    is LoadState.ERROR -> ErrorBanner(
        message = load.error.message,
        onRetry = viewModel::refresh,
    )
    else -> Unit
}
```

## Common tasks

### Refresh the list

```kotlin
paginator.reset(index = 0)
```

`reset()` cancels requests that are in flight and loads the page again from the network. Old cached pages are replaced only after the new data arrives, so a failed refresh does not leave the screen empty.

### Open the list at a position

For example, after a deep link or when returning to a screen:

```kotlin
paginator.reset(index = 250) // loads the page containing item 250 and its neighbours
```

### Update one item

Useful for likes, favourites or status changes:

```kotlin
fun onLikeClick(id: String) = viewModelScope.launch {
    val user = paginator.getItemById(id) ?: return@launch
    paginator.updateItem(id, user.copy(isLiked = !user.isLiked))
    runCatching { api.toggleLike(id) }
        .onFailure { paginator.updateItem(id, user) } // roll back on error
}
```

The change is written to the cache, and `itemsMap` emits the updated item. The page is not requested again.

### Clear the cache on logout

```kotlin
suspend fun logout() {
    paginators.forEach { it.close() }
    db.clearCache()
}
```

Close the paginators that use the database before calling `clearCache()`.

## Configuration

```kotlin
PaginatorConfig(
    pageSize = 60,         // items per page
    prefetchDistance = 1,  // how many pages to load and keep on each side of the visible one
    initialPageKey = 0,    // number of the first page in your API (0 or 1)
    maxPagesInDb = 3,      // how many pages to keep in the cache for this list
)
```

If your API counts pages from 1, set `initialPageKey = 1`.

## Other layouts

### Grid

```kotlin
PagingLayout(
    items = state.items,
    totalCount = state.totalCount,
    columns = 2,
    spanProvider = { item -> if (item.isWide) GridItemSpan(2) else GridItemSpan(1) },
    keyExtractor = { _, item -> item.id },
    onVisiblePageChanged = viewModel::onVisiblePageChanged,
) { _, item -> ProductCard(item) }
```

### Chat (reversed list)

With `isReversingPaging = true` the list starts at the bottom, and older messages load as the user scrolls up:

```kotlin
PagingLayout(
    items = state.messages,
    totalCount = state.totalCount,
    isReversingPaging = true,
    showItemsCounter = false,
    keyExtractor = { _, message -> message.id },
    onVisiblePageChanged = viewModel::onVisiblePageChanged,
) { _, message ->
    if (message != null) MessageBubble(message) else MessageSkeleton()
}
```

### Headers, separators, banners

If the list contains rows that do not come from the server, implement `PagingIndexedItem`. Server items and their placeholders keep their server index; decorative rows return `null`:

```kotlin
sealed interface FeedItem : PagingIndexedItem {
    data class Post(val post: PostDto, override val serverIndex: Int) : FeedItem
    data class Placeholder(override val serverIndex: Int) : FeedItem
    data class DateHeader(val date: LocalDate) : FeedItem {
        override val serverIndex: Int? = null
    }
}
```

Build this list in the ViewModel from `itemsMap`. `PagingLayout` uses `serverIndex` to work out the visible page, so headers do not shift the pagination. A plain list of your own items does not need this interface.

## How it works

1. The UI reports the visible page through `onVisiblePageChanged()`.
2. The paginator calculates which pages to keep (`page ± prefetchDistance`) and cancels requests outside this range.
3. Pages that are already in the cache are not requested again. Missing pages are loaded through `getPage`.
4. Results are written to SQLDelight in one transaction. Pages that are too far from the visible one are deleted.
5. `itemsMap` is a reactive query to the database, so any change in the cache reaches the UI.

All commands (`onVisiblePageChanged`, `reset`) are processed one by one in the order they were sent, so quick scrolling and repeated refreshes do not mix up the results.

## Tests

The library is covered by unit tests for `StablePaginator` and Compose UI tests for `PagingLayout`: cache-first loading, cancellation on scroll and reset, late responses after reset, network errors with cache, deep links, database pruning, single-item updates.

```bash
./gradlew :paging-core:jvmTest
```

Tests run in GitHub Actions on every push and pull request.

## License

```
Copyright 2026 Vladyslav Popov

Licensed under the Apache License, Version 2.0
```

See [LICENSE](LICENSE).
