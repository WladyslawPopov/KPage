package io.github.wladyslawpopov.kpage.core.paging.data

import kotlin.time.Duration.Companion.milliseconds

/** Number of lists (`queryKey`s) kept in one database until the app sets another with `setMaxLists`. */
const val DEFAULT_MAX_LISTS = 100

/**
 * Version of the database schema, embedded in the cache file name (`kpage_v<N>.db`).
 *
 * The cache is disposable, so there are no migrations: a changed schema simply uses a new file,
 * and files of other versions are deleted when a driver is created. This works the same way for
 * upgrades and downgrades. **Bump it whenever a `.sq` file changes in a released version.**
 */
internal const val SCHEMA_VERSION = 1

/** File name of the 1.x cache. It does not follow the versioned naming, so drivers delete it explicitly. */
internal const val LEGACY_DATABASE_NAME = "PagingDataBase"

/** SQLite sidecar files that belong to a database file. */
internal val SQLITE_SIDECAR_SUFFIXES = listOf("-wal", "-shm", "-journal")

/** Marks a lazy list that has not been laid out yet, so nothing is reported to the paginator. */
internal val NOT_LAID_OUT = IntRange(Int.MIN_VALUE, Int.MIN_VALUE)


/**
 * A hide followed by a show within this time is not leaving the list (fast tab switches). Nobody
 * can open an item, change it and come back that fast, so a real return is always refreshed.
 */
internal val RETURN_GRACE = 500.milliseconds

const val PAGE_SIZE = 60
