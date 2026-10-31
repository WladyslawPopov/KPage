package io.github.wladyslawpopov.kpage.core.paging

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.wladyslawpopov.kpage.cache.PagingDatabase
import io.github.wladyslawpopov.kpage.core.paging.data.LEGACY_DATABASE_NAME
import io.github.wladyslawpopov.kpage.core.paging.data.SQLITE_SIDECAR_SUFFIXES
import io.github.wladyslawpopov.kpage.core.paging.data.cache.databaseFileName
import io.github.wladyslawpopov.kpage.core.paging.data.cache.isStaleDatabaseFile
import java.io.File
import java.util.Properties

/**
 * File-based driver for the paging cache; the directory is chosen by the application.
 * The schema version is part of the file name (`<name>_v<N>.db`); files of other versions and the
 * 1.x cache (`~/.kpage/PagingDataBase.db`) are deleted on a best-effort basis. Write-ahead logging
 * and a busy timeout are enabled.
 */
fun createPagingDriver(directory: File, name: String = "kpage"): SqlDriver {
    directory.mkdirs()
    runCatching {
        directory.listFiles()
            ?.filter { isStaleDatabaseFile(it.name, name) }
            ?.forEach { it.delete() }
    }
    runCatching {
        val legacyDirectory = File(System.getProperty("user.home"), ".kpage")
        val legacyFile = "$LEGACY_DATABASE_NAME.db"
        (listOf(legacyFile) + SQLITE_SIDECAR_SUFFIXES.map { legacyFile + it })
            .forEach { File(legacyDirectory, it).delete() }
        // Removed only if nothing else is left there.
        legacyDirectory.delete()
    }
    // WAL lets the paginator write pages while the UI reads its snapshot, without blocking each other.
    val properties = Properties().apply {
        setProperty("journal_mode", "WAL")
        setProperty("busy_timeout", "5000")
    }
    val file = File(directory, databaseFileName(name))
    return JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}", properties, PagingDatabase.Schema)
}

/** In-memory driver for tests and previews. */
fun createInMemoryPagingDriver(): SqlDriver =
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { PagingDatabase.Schema.create(it) }
