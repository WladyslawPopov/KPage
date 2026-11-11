# Testing the demo

The logic of the demo lives in shared Kotlin (`domain`, `data`, `presentation`), so it is tested once
and runs on every platform that has tests: **106 tests on the JVM, 92 on the iOS simulator** (the
JVM also runs the tests that need a JDBC database). The UI of the platforms (Compose, SwiftUI) is
not covered yet; see [What is not covered](#what-is-not-covered).

```bash
cd demo
./gradlew :data:jvmTest :presentation:jvmTest                          # JVM
./gradlew :data:iosSimulatorArm64Test :presentation:iosSimulatorArm64Test   # iOS simulator, needs a Mac
```

CI runs both (`.github/workflows/tests.yml`). The tests of the library itself are in `paging-core`.

## How the tests are built

- **No mocking library.** Every dependency is a small hand-written fake that records what it was asked.
- **Time is controlled.** Engines get `TestTasks`, which runs their coroutines in one `runTest` scope,
  so a 400 ms search pause is `advanceTimeBy(400)`, not a sleep.
- **The paginator is faked at its interface.** `FakePaginator` counts `refresh()` and `updateItem()` and
  lets a test set the load state; lists with gaps are built with the library's `pagedListOf`.
- **Data tests use real parts where it is cheap:** an in-memory SQLite (`JdbcSqliteDriver`) for the demo
  database, Ktor `MockEngine` for NASA, `MapSettings` for preferences.

## What is covered

### Data (`data/src/commonTest`, `data/src/jvmTest`)

| Test | Checks |
|---|---|
| `SettingsRepositoryImplTest` (5) | the saved choice wins over the system language; an unsupported system language falls back to English; an unknown stored value falls back to the default; choices survive a restart |
| `NasaImagesRemoteTest` (9) | an error answer carries the status and NASA's `reason`; an HTML error page has no message; a body of another shape is `INVALID_RESPONSE`; one odd item does not fail a page; an apostrophe is dropped from exact filters but not from the text; blank filters are not sent |
| `FavoritesRepositoryImplTest` (5, JVM) | missing images are fetched; a refresh asks NASA only for new ids; an id NASA does not know is removed; a failed request keeps the id and reports the error |
| `ChatServerTest` (9, JVM) | sending twice with one client id makes one message; a refused send stores nothing; pages go from the newest message; deleting a message updates the chat preview; deleting a chat removes its messages and announces the change |

### Presentation (`presentation/src/commonTest`)

| Test | Checks |
|---|---|
| `PaginatorEngineTest` (8) | a new query closes the previous paginator; the same query keeps it; a load error is reported once; a refresh event renews the current paginator |
| `StateComponentTest` (13) | destroying the context cancels running tasks and disposes once; hiding does not; a component made after the destroy runs nothing; failures are mapped and reported; a local handler replaces the component's; siblings and children are isolated; `ErrorMapper` rules |
| `SpaceEngineTest` (9) | the list follows the search text only after a 400 ms pause; text is trimmed; filters apply at once; reset restores the opening query; "nothing found" offers a way back only for a changed query; layout is saved; a heart is saved, then applied to the cached item |
| `FavoritesEngineTest` (8) | selection mode; delete asks first; a confirmed delete removes ids and clears hearts in the Space lists; empty state; errors go to the page |
| `ChatsEngineTest` (9) | selection and delete as in favorites; a new chat is trimmed and cut to 40 characters; blank input creates nothing; creating and deleting are applied in order; empty state offers to create a chat |
| `MessagesEngineTest` (10) | date rows between days, at the end of a loaded run and not across a gap; unconfirmed local messages at the bottom; a server message with the local id replaces it and resolves it; the message menus; one delete at a time; refresh only for this chat |
| `ComposerEngineTest` (10) | the draft is restored and saved; a message is staged before it is sent; a failed send stays and reports; retry; no second send while one runs; at most 8 photos |
| `SettingsEngineTest` (4) | choices go to the repository; clearing the cache asks first |
| `SpaceDisplayTest` (5), `NavigationSnapshotCodecTest` (2) | display models of Space; saving and restoring the navigation |

## What is not covered

Said plainly, so the numbers above are not read as more than they are:

- **UI.** No Compose or SwiftUI tests, no screenshots.
- **On-device checks.** Gestures, keyboard, the photo picker and the like are checked by hand.
- **Platform code:** the `actual` implementations (system language, time zone, database drivers).
- **Repositories not tested yet:** `SpaceRepositoryImpl`, `ChatRepositoryImpl` (local outgoing messages
  and drafts in the database), `CacheRepositoryImpl`.
- **Engines not tested yet:** `SpaceDetailsEngine`, `ChatInfoEngine`, `LabEngine`, and the components
  that connect the engines to navigation.
- **Real network.** NASA is replaced by `MockEngine`; no test talks to the real API.
- **Benchmarks and instrumented tests.**
