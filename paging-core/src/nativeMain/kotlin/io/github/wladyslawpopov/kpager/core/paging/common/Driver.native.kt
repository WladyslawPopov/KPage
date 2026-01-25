package io.github.wladyslawpopov.kpager.core.paging.common

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import io.github.wladyslawpopov.kpager.cache.PagingDatabase
import io.github.wladyslawpopov.kpager.core.paging.data.DATABASE_NAME

actual fun getDriver(): app.cash.sqldelight.db.SqlDriver {
    return NativeSqliteDriver(
        schema = PagingDatabase.Schema,
        name = DATABASE_NAME,
        maxReaderConnections = 4
    )
}
