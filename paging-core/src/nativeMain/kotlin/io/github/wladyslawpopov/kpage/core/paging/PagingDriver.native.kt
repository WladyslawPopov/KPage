package io.github.wladyslawpopov.kpage.core.paging

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import co.touchlab.sqliter.DatabaseFileContext
import io.github.wladyslawpopov.kpage.cache.PagingDatabase
import io.github.wladyslawpopov.kpage.core.paging.data.LEGACY_DATABASE_NAME
import io.github.wladyslawpopov.kpage.core.paging.data.cache.databaseFileName
import io.github.wladyslawpopov.kpage.core.paging.data.cache.isStaleDatabaseFile
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/**
 * File-based driver for the paging cache, stored in `Caches/kpage`: the cache is disposable, so it
 * is not backed up to iCloud and the system may purge it when storage is low. The schema
 * version is part of the file name (`<name>_v<N>.db`); files of other versions and of earlier
 * locations are deleted on a best-effort basis. The driver uses write-ahead logging and a busy
 * timeout by default.
 */
@OptIn(ExperimentalForeignApi::class)
fun createPagingDriver(name: String = "kpage"): SqlDriver {
    val directory = "${NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true).first()}/kpage"
    val fileManager = NSFileManager.defaultManager
    runCatching {
        fileManager.createDirectoryAtPath(directory, withIntermediateDirectories = true, attributes = null, error = null)
        fileManager.contentsOfDirectoryAtPath(directory, error = null)
            .orEmpty()
            .filterIsInstance<String>()
            .filter { isStaleDatabaseFile(it, name) }
            .forEach { fileManager.removeItemAtPath("$directory/$it", error = null) }
    }
    deleteEarlierLocations(fileManager)
    // WAL (on by default here) lets the paginator write pages while the UI reads its snapshot,
    // without blocking each other.
    return NativeSqliteDriver(
        schema = PagingDatabase.Schema,
        name = databaseFileName(name),
        onConfiguration = { config ->
            config.copy(extendedConfig = config.extendedConfig.copy(basePath = directory))
        },
        maxReaderConnections = 4,
    )
}

// The 1.x cache in the default SQLDelight location, and `Application Support/kpage` used before
// the cache moved to `Caches`.
@OptIn(ExperimentalForeignApi::class)
private fun deleteEarlierLocations(fileManager: NSFileManager) {
    runCatching { DatabaseFileContext.deleteDatabase(LEGACY_DATABASE_NAME) }
    runCatching {
        val applicationSupport =
            NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first()
        fileManager.removeItemAtPath("$applicationSupport/kpage", error = null)
    }
}

/** In-memory driver for tests and previews. */
fun createInMemoryPagingDriver(): SqlDriver = inMemoryDriver(PagingDatabase.Schema)
