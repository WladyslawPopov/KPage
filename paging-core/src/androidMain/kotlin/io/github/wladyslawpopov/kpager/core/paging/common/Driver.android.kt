package io.github.wladyslawpopov.kpager.core.paging.common

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.github.wladyslawpopov.kpager.cache.PagingDatabase
import io.github.wladyslawpopov.kpager.core.paging.data.DATABASE_NAME
import org.koin.mp.KoinPlatform.getKoin

actual fun getDriver(): SqlDriver {
    val context: Context = getKoin().get()
    return AndroidSqliteDriver(PagingDatabase.Schema, context, DATABASE_NAME)
}
