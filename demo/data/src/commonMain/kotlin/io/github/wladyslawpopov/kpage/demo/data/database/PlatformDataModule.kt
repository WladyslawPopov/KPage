package io.github.wladyslawpopov.kpage.demo.data.database

import org.koin.core.module.Module

/** File name of the demo's own database (favorites), next to the cache database of KPage. */
internal const val DEMO_DATABASE_NAME = "demo.db"

/** Name of the key-value store of the user's choices. */
internal const val SETTINGS_NAME = "kpage_demo_settings"

/**
 * Bindings that need the platform: the cache database of KPage, the demo's own database and the
 * key-value store of the settings.
 */
internal expect val platformDataModule: Module
