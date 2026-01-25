package io.github.wladyslawpopov.kpager.core.paging.common

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.wladyslawpopov.kpager.cache.PagingDatabase

actual fun getDriver(): SqlDriver {
    val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    PagingDatabase.Schema.create(driver)
    return driver
}
