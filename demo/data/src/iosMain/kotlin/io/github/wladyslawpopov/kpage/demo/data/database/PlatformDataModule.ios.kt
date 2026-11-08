package io.github.wladyslawpopov.kpage.demo.data.database

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import io.github.wladyslawpopov.kpage.cache.PagingDatabase
import io.github.wladyslawpopov.kpage.core.paging.createPagingDriver
import io.github.wladyslawpopov.kpage.demo.data.db.DemoDatabase
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

internal actual val platformDataModule: Module = module {
    single { PagingDatabase(createPagingDriver()) }
    single { DemoDatabase(NativeSqliteDriver(DemoDatabase.Schema, DEMO_DATABASE_NAME)) }
    single<Settings> { NSUserDefaultsSettings(NSUserDefaults(suiteName = SETTINGS_NAME)) }
}
