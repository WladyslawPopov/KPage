package io.github.wladyslawpopov.kpage.demo.data.database

import org.koin.core.module.Module

/** Bindings that need the platform: the cache database of KPage. */
internal expect val platformDataModule: Module
