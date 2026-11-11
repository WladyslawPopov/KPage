# KPage Demo

A complete app on [KPage](../README.md): **NASA image search, favorites, a chat with a mock server and settings, on Android (Jetpack Compose) and iOS (SwiftUI)**. All the logic is shared Kotlin, and the UI of each platform is fully native.

**About the app.** It brings together the practices I use in production: layers with clear responsibilities, MVI screens, one engine that wires a paginator to a screen, and a mock server for the cases a real API does not give. My Compose Multiplatform apps are built on Decompose; here I took the component approach of Arkadii Ivanov, the author of Decompose, and carried it over to a fully native UI on top of shared Kotlin logic. It is my own take on that idea. The library is proven by thousands of users; the shared logic of the demo is covered by tests ([what is covered](TESTING.md)), and its UI is checked by hand.

**On this page:** [Run it](#run-it) · [How it is built](#how-it-is-built) · [Screens](#screens) · [Tests](#tests)

## What it shows

| In the library | Where to see it |
|---|---|
| A 10 000-row list with placeholders and an offline cache | Space |
| A new paginator per query, with a pause while typing | Space search and filters |
| `updateItem`: one change in every list at once | the heart in Space, Details and Favorites |
| A "server" that is not an API with `totalCount`: ids first, items fetched | Favorites |
| `decorate`: date rows and local rows in a reversed list | Chat room |
| Cache statistics and clearing under open lists | Settings |

## Run it

1. Open the **`demo/`** folder in Android Studio (not the repository root). The demo builds KPage from this repository's sources (`includeBuild("..")`), so a change in the library shows up at once.
2. **Android:** run `androidApp`.
3. **iOS:** open `iosApp/iosApp.xcodeproj` in Xcode. The project is already set up: the only thing to set is your Apple Team ID (`TEAM_ID`, see `iosApp/Configuration/Config.xcconfig`).
4. **Desktop:** `desktopApp` exists, but it is secondary: it is not the goal of the demo.

NASA Image Library needs no API key.

## How it is built

```
domain        models, errors and repository interfaces
data          implementations (internal): NASA remote, repositories, the demo's own database, mock server
ui-core       pure Kotlin: theme tokens, string keys, models of widgets
presentation  pure Kotlin: components, engines, screen state, navigation contract
sharedLogic   the app layer: DI (Koin), strings, bridges to Swift
androidApp / iosApp / desktopApp    native UI
```

`presentation` knows only `domain` and `ui-core`; `data` is visible only to `sharedLogic` through DI.

**One screen is three things:**

- a **component** holds the screen's single immutable state, receives intents, owns its coroutines and lives with the screen's lifecycle;
- **engines** hold the business scenarios (search, selection, sending a message);
- the **platform screen** draws the state and sends intents; it knows nothing about subscriptions or lifecycle.

**Paging.** A repository creates a `StablePaginator` for a query. [`PaginatorEngine`](presentation/src/commonMain/kotlin/io/github/wladyslawpopov/kpage/demo/presentation/paginator/PaginatorEngine.kt) keeps one paginator per query, mirrors it into the screen state and reports a load error once. It is a **sample of wiring** into this app's architecture, not a part of the library's API.

**Kotlin and Swift.** The list is read as `PagingRows` by index, so it crosses to SwiftUI by reference, without a converted copy. Each platform has one list view that reports the visible rows to the paginator through `PagingListTracker`: [`PagedGrid.kt`](androidApp/src/main/kotlin/io/github/wladyslawpopov/kpage/demo/android/ui/paging/PagedGrid.kt) and [`PagedGrid.swift`](iosApp/iosApp/Paging/PagedGrid.swift).

<details>
<summary>More about the architecture</summary>

- Remote (`data`): one per API; one private `get { }` does what is common to every request, public methods only add their parameters. Failures leave it as domain errors: `ServerException` (with the server's text), `NetworkException`.
- A network failure (no connection, a timeout) is repeated once after a second: after sleep the network is often not up yet. A server answer, even an error, is not repeated. On iOS the system holds a request until there is a network (`waitsForConnectivity`).
- The paginator refreshes once when it is created; after that the app decides when to refresh. The demo does it on a pull, a changed query and the signals of its repositories (a changed favorite, a cleared cache). Any other reason, such as the return to a screen, is one `refresh()` call.
- Ownership: `ComponentContext` is moved by the platform (shown, hidden, destroyed); `StateComponent` cancels all coroutines of the screen on destroy.

</details>

## Screens

Each chapter: what the screen is, how it is built, what is done and what is left for later.

### Space

The start tab: search NASA's image library.

<!-- Full video of the listing (Android | iOS): the whole range, not one path. Paste the GitHub video URLs here. -->

- **What it does:** a list or a grid (the choice is remembered); search that follows the text after a 400 ms pause; filters by NASA center and years; chips of the active filters; a heart on every image; a tap opens the Details. From the Details a field (author, place, keyword, album) opens the same screen filtered by it.
- **How it works:** a `SpaceQuery` (text and filters) is the query of the list: a changed query is a new paginator. [`SpaceEngine`](presentation/src/commonMain/kotlin/io/github/wladyslawpopov/kpage/demo/presentation/screens/space/listing/SpaceEngine.kt) holds the search pause, the filters and the heart (the database first, then `updateItem`). NASA gives only the first 10 000 results, so `totalCount = min(total_hits, 10000)`.
- **Done:** all of the above on Android and iOS; the empty result offers "Reset" when a search or a filter is set.

### Details and the image viewer

- **What it does:** the picture, the title (long press copies), the description (selectable), fields as cards, keywords and albums as chips, the image's format, size and NASA ID. A tap on the picture opens a full-screen viewer: pinch, double tap, drag down or up to close.
- **How it works:** everything that is drawn comes ready in `SpaceDetailsState.display`, so the platform screens build no lists. The heart here changes the cache of every list through `PagingDatabase.updateItem`, without a paginator.
- **Done:** both platforms; pull to refresh; "no such image" is an empty state.

### Favorites

<!-- Full video of the favorites (Android | iOS). Paste the GitHub video URLs here. -->

- **What it does:** one column of your favorites; a long press starts selection, a tap toggles; delete one by a swipe or many from the bar, always after a confirmation.
- **How it works:** NASA has no favorites, so the demo's own database stands in for the server and keeps only ids. [`FavoritesRepositoryImpl`](data/src/commonMain/kotlin/io/github/wladyslawpopov/kpage/demo/data/favorites/FavoritesRepositoryImpl.kt) fetches the images of ids that have none yet, one request each (an id NASA does not know is removed; a failed request keeps it). Every change is announced as a signal and the list refreshes on it, whichever screen made it.
- **Done:** both platforms.

### Chat

<!-- Full video of the chat and the settings (Android | iOS). Paste the GitHub video URLs here. -->

- **What it does:** a list of chats (create, select, delete) and a room: messages from the newest, date rows, sending with a retry for failed ones, a draft that survives a restart, up to 8 photos from the gallery, copying and deleting messages.
- **How it works:** there is no backend: [`ChatServer`](data/src/commonMain/kotlin/io/github/wladyslawpopov/kpage/demo/data/chat/ChatServer.kt) is a server inside the app, with its own tables, and every call goes through a mock that adds delay, failures and offline. The other side answers by a scenario chosen when the chat is created (`CHATTY`, `HISTORIAN` with hundreds of messages, `FLAKY` that refuses part of the sends), and only while the app is open. A message is saved locally first and goes to the server with its local id, so a repeated send makes one message and the server's copy replaces the local one without the row blinking. In the room, `decorate` adds the unconfirmed local messages and the date rows ([`MessagesEngine`](presentation/src/commonMain/kotlin/io/github/wladyslawpopov/kpage/demo/presentation/screens/chat/room/MessagesEngine.kt)).
- **Done:** the list and the room on both platforms.
- **Next:** sharing a Space image or a favorite into a chat, links in messages, more scenarios.

### Settings

- **What it does:** theme (system, light, dark), language (English, Polski, Українська, Русский) applied at once, and the pagination cache: how many lists and how much data, with a "Clear cache" button after a confirmation.
- **How it works:** the choices live in the repository as flows that the platform roots follow, so a change shows without recreating screens. Clearing the cache is announced as a signal, and the screens with lists refresh.
- **Done:** both platforms.
- **Next:** a control for the number of cached lists (`setMaxLists`).

### Lab

Planned. The idea: cases a real API does not give, such as no `totalCount`, an error in the middle of a scroll, a slow network, offline with a cache, pages that shift on the server, started from presets, with a visible log of what the paginator requests.

## Tests

Shared logic (repositories, engines, the component base) is covered on the JVM and the iOS simulator. The numbers and what each test checks: [TESTING.md](TESTING.md).

```bash
cd demo
./gradlew :data:jvmTest :presentation:jvmTest
```
