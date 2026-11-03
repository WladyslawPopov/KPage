package io.github.wladyslawpopov.kpage.demo.data.database

import io.github.wladyslawpopov.kpage.cache.PagingDatabase
import io.github.wladyslawpopov.kpage.core.paging.createPagingDriver
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformDataModule: Module = module {
    single { PagingDatabase(createPagingDriver()) }
}
