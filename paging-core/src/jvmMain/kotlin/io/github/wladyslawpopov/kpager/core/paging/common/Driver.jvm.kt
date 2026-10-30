package io.github.wladyslawpopov.kpager.core.paging.common

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.wladyslawpopov.kpager.cache.PagingDataBase
import io.github.wladyslawpopov.kpager.core.paging.data.DATABASE_NAME
import java.io.File

/** System property to override the cache file location, e.g. `-Dkpage.db.path=/path/app-cache.db`. */
const val DB_PATH_PROPERTY = "kpage.db.path"

/**
 * File-based SQLite driver so the cache survives app restarts on Desktop.
 * Default location: `~/.kpage/PagingDataBase.db`.
 * Schema creation and migrations are handled by SQLDelight via `PRAGMA user_version`.
 */
actual fun getDriver(): SqlDriver {
    val file = System.getProperty(DB_PATH_PROPERTY)
        ?.let(::File)
        ?: File(System.getProperty("user.home"), ".kpage/$DATABASE_NAME.db")
    file.parentFile?.mkdirs()

    return JdbcSqliteDriver(
        url = "jdbc:sqlite:${file.absolutePath}",
        schema = PagingDataBase.Schema,
    )
}
