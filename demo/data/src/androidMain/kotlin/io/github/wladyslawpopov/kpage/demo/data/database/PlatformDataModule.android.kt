package io.github.wladyslawpopov.kpage.demo.data.database

import android.content.Context
import io.github.wladyslawpopov.kpage.cache.PagingDatabase
import io.github.wladyslawpopov.kpage.core.paging.createPagingDriver
import org.koin.core.module.Module
import org.koin.dsl.module

// The app registers its Context in the platform module of initKoin.
internal actual val platformDataModule: Module = module {
    single { PagingDatabase(createPagingDriver(get<Context>())) }
}
