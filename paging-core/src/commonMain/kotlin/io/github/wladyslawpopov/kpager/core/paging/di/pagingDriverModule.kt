package io.github.wladyslawpopov.kpager.core.paging.di

import io.github.wladyslawpopov.kpager.cache.PagingDataBase
import io.github.wladyslawpopov.kpager.core.paging.common.getDriver
import org.koin.dsl.module

val pagingDriverModule = module {
    single {
        val driver = getDriver()
        PagingDataBase(driver)
    }
}
