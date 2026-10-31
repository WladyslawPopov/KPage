package io.github.wladyslawpopov.kpage.core.paging

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.github.wladyslawpopov.kpage.cache.PagingDatabase
import io.github.wladyslawpopov.kpage.core.paging.data.LEGACY_DATABASE_NAME
import io.github.wladyslawpopov.kpage.core.paging.data.cache.databaseFileName
import io.github.wladyslawpopov.kpage.core.paging.data.cache.isStaleDatabaseFile

/**
 * File-based driver for the paging cache. The schema version is part of the file name
 * (`<name>_v<N>.db`); files of other versions and the 1.x cache are deleted on a best-effort basis.
 */
fun createPagingDriver(context: Context, name: String = "kpage"): SqlDriver {
    val appContext = context.applicationContext
    runCatching {
        appContext.databaseList()
            .filter { isStaleDatabaseFile(it, name) }
            .forEach { appContext.deleteDatabase(it) }
        // The 1.x cache; deleteDatabase also removes its sidecar files and ignores a missing one.
        appContext.deleteDatabase(LEGACY_DATABASE_NAME)
    }
    return AndroidSqliteDriver(
        schema = PagingDatabase.Schema,
        context = appContext,
        name = databaseFileName(name),
        callback = WalCallback(),
    )
}

/** In-memory driver for tests and previews. */
fun createInMemoryPagingDriver(context: Context): SqlDriver =
    AndroidSqliteDriver(PagingDatabase.Schema, context.applicationContext, null)

// WAL lets the paginator write pages while the UI reads its snapshot, without blocking each other.
// It is not the default before Android 9.
private class WalCallback : AndroidSqliteDriver.Callback(PagingDatabase.Schema) {
    override fun onConfigure(db: SupportSQLiteDatabase) {
        db.enableWriteAheadLogging()
    }
}
