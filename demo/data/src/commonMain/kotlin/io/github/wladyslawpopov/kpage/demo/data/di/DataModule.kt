package io.github.wladyslawpopov.kpage.demo.data.di

import io.github.wladyslawpopov.kpage.demo.data.cache.CacheRepositoryImpl
import io.github.wladyslawpopov.kpage.demo.data.chat.ChatRepositoryImpl
import io.github.wladyslawpopov.kpage.demo.data.database.platformDataModule
import io.github.wladyslawpopov.kpage.demo.data.lab.LabRepositoryImpl
import io.github.wladyslawpopov.kpage.demo.data.mock.MockServer
import io.github.wladyslawpopov.kpage.demo.data.network.createHttpClient
import io.github.wladyslawpopov.kpage.demo.data.network.createHttpEngine
import io.github.wladyslawpopov.kpage.demo.data.settings.SettingsRepositoryImpl
import io.github.wladyslawpopov.kpage.demo.data.space.SpaceRepositoryImpl
import io.github.wladyslawpopov.kpage.demo.domain.cache.CacheRepository
import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatRepository
import io.github.wladyslawpopov.kpage.demo.domain.lab.LabRepository
import io.github.wladyslawpopov.kpage.demo.domain.settings.SettingsRepository
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceRepository
import kotlinx.serialization.json.Json
import org.koin.dsl.module

/** The data layer for DI; nothing else of the layer is visible outside. */
val dataModule = module {
    includes(platformDataModule)

    single { Json { ignoreUnknownKeys = true; explicitNulls = false } }
    single { createHttpClient(createHttpEngine(), get()) }
    single { MockServer() }

    single<SpaceRepository> { SpaceRepositoryImpl() }
    single<ChatRepository> { ChatRepositoryImpl() }
    single<LabRepository> { LabRepositoryImpl() }
    single<CacheRepository> { CacheRepositoryImpl() }
    single<SettingsRepository> { SettingsRepositoryImpl() }
}
