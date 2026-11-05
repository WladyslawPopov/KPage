package io.github.wladyslawpopov.kpage.demo.data.database

import org.koin.core.module.Module

/** File name of the demo's own database (favorites), next to the cache database of KPage. */
internal const val DEMO_DATABASE_NAME = "demo.db"

/** Bindings that need the platform: the cache database of KPage and the demo's own database. */
internal expect val platformDataModule: Module
