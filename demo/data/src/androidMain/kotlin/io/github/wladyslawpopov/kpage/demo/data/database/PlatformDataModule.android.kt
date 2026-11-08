package io.github.wladyslawpopov.kpage.demo.data.database

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import io.github.wladyslawpopov.kpage.cache.PagingDatabase
import io.github.wladyslawpopov.kpage.core.paging.createPagingDriver
import io.github.wladyslawpopov.kpage.demo.data.db.DemoDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

// The app registers its Context in the platform module of initKoin.
internal actual val platformDataModule: Module = module {
    single { PagingDatabase(createPagingDriver(get<Context>())) }
    single { DemoDatabase(AndroidSqliteDriver(DemoDatabase.Schema, get<Context>(), DEMO_DATABASE_NAME)) }
    single<Settings> {
        SharedPreferencesSettings(get<Context>().getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE))
    }
}
