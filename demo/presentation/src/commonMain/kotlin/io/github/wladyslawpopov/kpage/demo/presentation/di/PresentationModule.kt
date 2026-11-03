package io.github.wladyslawpopov.kpage.demo.presentation.di

import io.github.wladyslawpopov.kpage.demo.presentation.navigation.ScreenFactory
import org.koin.dsl.module

val presentationModule = module {
    single { ScreenFactory() }
}
