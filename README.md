# K-Pager

**K-Pager** is a production-ready, highly optimized, and caching pagination library for **Kotlin Multiplatform** and **Compose Multiplatform**.

Unlike standard paging libraries, K-Pager is built with local-first and offline-first architectures in mind. It uses **SQLDelight** under the hood to automatically cache your pages, providing instant cold starts, optimistic UI updates, and seamless background network synchronization.

## ✨ Features
* **Multiplatform Support:** Works seamlessly on Android, iOS, and Desktop (JVM).
* **Built-in Local Cache:** Pages are automatically saved to a local SQLite database.
* **Smart DB Pruning:** Automatically deletes old pages when the user scrolls too far, preventing Out-Of-Memory (OOM) errors and database bloating.
* **Optimistic Updates:** Update a single item (e.g., a "like" button) in the DB and UI instantly, without reloading the page.
* **Compose Ready:** Comes with a highly customizable `PagingLayout` (based on `LazyGrid`) that handles prefetching thresholds automatically.
* **Versatile:** Easily supports standard lists, reverse lists (for chats/messengers), and heterogeneous lists (mixing data with promo blocks or date separators).

## 📦 Installation

Add the dependency to your `build.gradle.kts` (in your `commonMain` source set):

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            // Replace with the actual version
            implementation("io.github.wladyslawpopov.kpager:paging-core:1.0.0") 
        }
    }
}
```

*(Note: Ensure you have SQLDelight drivers configured for your target platforms, as K-Pager requires an `SqlDriver` instance to work).*

## 🚀 Quick Start (Basic Usage)

### 1. Create the Paginator
Create a `StablePaginator` in your ViewModel or Presenter. Pass your network call and a single `SqlDriver` instance.

```kotlin
val paginator = StablePaginator(
    serializer = MyItem.serializer(),
    queryKey = "users_list", 
    config = PaginatorConfig(pageSize = 60),
    getPage = { page -> 
        // Your network call returning PagerPayload
        apiService.getUsersPage(page) 
    },
    idExtractor = { item -> item.id },
    driver = mySqlDriver // Inject your SQLDelight driver here
)

// Trigger the first load
paginator.reset(index = 0)
```

### 2. Connect to Compose UI
Collect the StateFlows and pass them to the `PagingLayout`.

```kotlin
@Composable
fun UsersScreen(viewModel: MyViewModel) {
    val itemsMap by viewModel.paginator.itemsMap.collectAsState()
    val totalCount by viewModel.paginator.totalCount.collectAsState()

    PagingLayout(
        items = itemsMap.values.toList(),
        totalCount = totalCount,
        pageSize = 60,
        keyExtractor = { index, item -> item?.id ?: "placeholder_$index" },
        onPrefetch = { page -> viewModel.paginator.onPrefetch(page) }
    ) { index, item ->
        if (item != null) {
            UserCard(item)
        } else {
            LoadingPlaceholder()
        }
    }
}
```

## 🛠 Advanced Usage

### Mixing Items (Separators, Ads, Promos)
K-Pager makes it incredibly easy to map your domain items into UI models. You don't need complex `PagingData` transformations. Just map the Flow!

```kotlin
val uiItemsFlow = paginator.itemsMap.map { map ->
    val uiList = mutableListOf<UiItem>()
    map.values.forEachIndexed { index, item ->
        // Inject a promo banner every 10 items
        if (index > 0 && index % 10 == 0) {
            uiList.add(UiItem.PromoBanner)
        }
        if (item != null) uiList.add(UiItem.UserItem(item))
    }
    uiList
}
```

### Reverse Scrolling (Chats & Messengers)
Building a chat? `PagingLayout` supports reverse layouts out-of-the-box. Just set `isReversingPaging = true`, and the layout will start from the bottom, fetching older messages as the user scrolls up.

```kotlin
PagingLayout(
    items = chatMessages,
    totalCount = totalMessagesCount,
    isReversingPaging = true, // Magic happens here!
    onPrefetch = { page -> loadHistory(page) },
    // ...
)
```

---

## 💻 Building the Project Locally

This is a Kotlin Multiplatform project targeting Android, iOS, and Desktop (JVM).

* `/composeApp` is for code that will be shared across your Compose Multiplatform applications.
* `/iosApp` contains the iOS application entry point.

### Build and Run Android Application
To build and run the development version of the Android app, use the run configuration from the run widget in your IDE’s toolbar or build it directly from the terminal:
- on macOS/Linux:
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- on Windows:
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

### Build and Run Desktop (JVM) Application
To build and run the development version of the desktop app, use the run configuration from the run widget in your IDE’s toolbar or run it directly from the terminal:
- on macOS/Linux:
  ```shell
  ./gradlew :composeApp:run
  ```
- on Windows:
  ```shell
  .\gradlew.bat :composeApp:run
  ```

### Build and Run iOS Application
To build and run the development version of the iOS app, use the run configuration from the run widget in your IDE’s toolbar or open the `/iosApp` directory in Xcode and run it from there.
