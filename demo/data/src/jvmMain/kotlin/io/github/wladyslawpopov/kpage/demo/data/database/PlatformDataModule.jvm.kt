package io.github.wladyslawpopov.kpage.demo.data.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.wladyslawpopov.kpage.cache.PagingDatabase
import io.github.wladyslawpopov.kpage.core.paging.createPagingDriver
import io.github.wladyslawpopov.kpage.demo.data.db.DemoDatabase
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File
import java.util.Properties

internal actual val platformDataModule: Module = module {
    val directory = File(System.getProperty("user.home"), ".kpage-demo")
    single { PagingDatabase(createPagingDriver(directory)) }
    single {
        directory.mkdirs()
        val file = File(directory, DEMO_DATABASE_NAME)
        DemoDatabase(JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}", Properties(), DemoDatabase.Schema))
    }
}
