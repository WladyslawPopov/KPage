package io.github.wladyslawpopov.kpage.demo.shared.di

import io.github.wladyslawpopov.kpage.demo.presentation.navigation.ScreenFactory
import io.github.wladyslawpopov.kpage.demo.shared.state.AppSettingsObserver
import org.koin.mp.KoinPlatform

/** Starts DI from Swift. */
fun startKoinIos() = initKoin()

/** The factory of screen components for the Swift navigator. */
fun screenFactory(): ScreenFactory = KoinPlatform.getKoin().get()

/** The theme and the language of the app for the Swift root. */
fun appSettingsObserver(): AppSettingsObserver = AppSettingsObserver(KoinPlatform.getKoin().get())
