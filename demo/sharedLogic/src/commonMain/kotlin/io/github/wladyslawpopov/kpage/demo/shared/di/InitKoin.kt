package io.github.wladyslawpopov.kpage.demo.shared.di

import io.github.wladyslawpopov.kpage.demo.data.di.dataModule
import io.github.wladyslawpopov.kpage.demo.presentation.di.presentationModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

/** Starts DI of the whole app; each platform adds its own bindings in [platform]. */
fun initKoin(platform: Module = module { }) {
    startKoin { modules(platform, dataModule, presentationModule) }
}
