# KPager

**KPager** is a production-ready, highly optimized, and caching pagination library for **Kotlin Multiplatform** and **Compose Multiplatform**.

Unlike standard paging libraries, KPager is built with local-first and offline-first architectures in mind. It uses **SQLDelight** under the hood to automatically cache your pages, providing instant cold starts, optimistic UI updates, and seamless background network synchronization.

## ✨ Features
* **Multiplatform Support:** Works seamlessly on Android, iOS, and Desktop (JVM).
* **Built-in Local Cache:** Pages are automatically saved to a local SQLite database.
* **Smart DB Pruning:** Automatically deletes old pages when the user scrolls too far, preventing Out-Of-Memory (OOM) errors and database bloating.
* **Optimistic Updates:** Update a single item (e.g., a "like" button) in the DB and UI instantly, without reloading the page.
* **Compose Ready:** Comes with a highly customizable `PagingLayout` (based on `LazyGrid`) that handles prefetching thresholds automatically.
* **Versatile:** Easily supports standard lists, reverse lists (for chats/messengers), and heterogeneous lists (mixing data with promo blocks or date separators).

## 📦 Installation (Maven Local)

Since this is a Kotlin Multiplatform library with iOS support, it currently relies on local publishing.

**Step 1. Clone and Publish Locally**
Clone this repository to your machine and publish the artifacts to your local Maven repository:
```bash
git clone [https://github.com/WladyslawPopov/KPager.git](https://github.com/WladyslawPopov/KPager.git)
cd KPager
./gradlew publishToMavenLocal
```

**Step 2. Setup your project's repositories**
In your target project, ensure `mavenLocal()` is added to your repository list in `settings.gradle.kts` (or root `build.gradle.kts`):

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenLocal() // Add this at the top
        google()
        mavenCentral()
    }
}
```

**Step 3. Add the dependency**
Add the dependency to your module's `build.gradle.kts` (in your `commonMain` source set):

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.wladyslawpopov:paging-core:1.0.1") 
        }
    }
}
```

## 🛠 Setup & Initialization (Dependency Injection)

KPager requires a SQLDelight database instance to handle offline caching. You must initialize this database using dependency injection.

### Koin Integration
If you are using [Koin](https://insert-koin.io/), you can provide the `PagingDataBase` like this:

```kotlin
import io.github.wladyslawpopov.kpager.cache.PagingDataBase
import io.github.wladyslawpopov.kpager.core.paging.common.getDriver
import org.koin.dsl.module

val pagingDriverModule = module {
    single {
        val driver = getDriver()
        PagingDataBase(driver)
    }
}
```
*Don't forget to include `pagingDriverModule` when starting your Koin application.*

## 🚀 Quick Start (Basic Usage)

### 1. Create the Paginator
Create a `StablePaginator` in your ViewModel or Presenter. Pass your network call and the injected `PagingDataBase` instance.

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
    db = myPagingDatabase // Inject your Koin database instance here
)

// Trigger the first load
paginator.reset(index = 0)
```

### 2. Prepare UI State Outside Compose
The current `Paginator` API exposes `itemsMap`: its keys are global server indices, not positions within the cached subset. Your Store/ViewModel prepares the list for presentation and exposes it as UI state. `PagingLayout` receives that prepared list directly.

Keep the conversion outside the composable and off the main thread. Preserve server positions and represent missing data with placeholders. Do not use `itemsMap.values.toList()`: removing gaps changes the meaning of the visible index after cache eviction. Avoid allocating a full list of empty slots up to a distant index on every update; use a list adapter that reads missing positions from the indexed snapshot on demand.

`items` in the following example belongs to the application's UI state. It is not an additional property of `Paginator`: the current library exposes `itemsMap`, not a ready-made list flow.

### 3. Connect to Compose UI
The UI renders the prepared state and reports the visible server page through one callback. The paginator decides which neighboring pages to load, uses cached pages, cancels obsolete requests, and retains data around the current position. `reset()` explicitly refreshes data.

```kotlin
@Composable
fun UsersScreen(viewModel: MyViewModel) {
    val state by viewModel.uiState.collectAsState()

    PagingLayout(
        items = state.items,
        totalCount = state.totalCount,
        pageSize = 60,
        keyExtractor = { _, item -> item.id },
        onVisiblePageChanged = viewModel::onVisiblePageChanged,
    ) { _, item ->
        if (item != null) {
            UserCard(item)
        } else {
            LoadingPlaceholder()
        }
    }
}
```

The Store/ViewModel forwards `onVisiblePageChanged(page)` to `paginator.onVisiblePageChanged(page)`. The UI does not select pages to fetch, manage cache eviction, or rebuild the pagination data structure.

## 🧠 Advanced Usage

### Mixing Items (Separators, Ads, Promos)
For mixed lists, implement `PagingIndexedItem` on your UI items. Keep the server index on data rows and placeholders; return null for decorative rows. Ordinary lists need no interface or wrapper.

```kotlin
sealed interface UiItem : PagingIndexedItem {
    data class UserItem(val user: User, override val serverIndex: Int) : UiItem
    data class Placeholder(override val serverIndex: Int) : UiItem
    data object PromoBanner : UiItem {
        override val serverIndex: Int? = null
    }
}
```

Build these UI items in the Store/ViewModel's presentation adapter. A data row keeps the key from `itemsMap` as its `serverIndex`; a placeholder keeps the same index while data is missing; a banner or separator has no server index. The index travels with the item, so no parallel index list or mapping callback is needed. Pass the resulting UI list directly as `items = state.items`.

### Reverse Scrolling (Chats & Messengers)
Building a chat? `PagingLayout` supports reverse layouts out-of-the-box. Just set `isReversingPaging = true`, and the layout will start from the bottom, fetching older messages as the user scrolls up.

```kotlin
PagingLayout(
    items = chatMessages,
    totalCount = totalMessagesCount,
    isReversingPaging = true, // Magic happens here!
    onVisiblePageChanged = { page -> paginator.onVisiblePageChanged(page) },
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
