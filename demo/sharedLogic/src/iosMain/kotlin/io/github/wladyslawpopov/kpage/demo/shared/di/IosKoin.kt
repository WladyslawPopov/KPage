package io.github.wladyslawpopov.kpage.demo.shared.di

import io.github.wladyslawpopov.kpage.demo.presentation.navigation.ScreenFactory
import org.koin.mp.KoinPlatform

/** Starts DI from Swift. */
fun startKoinIos() = initKoin()

/** The factory of screen components for the Swift navigator. */
fun screenFactory(): ScreenFactory = KoinPlatform.getKoin().get()
