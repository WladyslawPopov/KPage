package io.github.wladyslawpopov.kpager.core.paging.common

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.wladyslawpopov.kpager.cache.PagingDataBase

actual fun getDriver(): SqlDriver {
    val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    PagingDataBase.Schema.create(driver)
    return driver
}
