package io.github.wladyslawpopov.kpage.demo.shared.strings

import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import org.jetbrains.compose.resources.getString

/**
 * Strings for SwiftUI. Compose Resources read a string only in a suspend call, so all of them are
 * loaded once at start and then read synchronously.
 */
object IosStrings {
    private var strings: Map<AppStringKey, String> = emptyMap()

    suspend fun load() {
        strings = allKeys().associateWith { getString(it.resource()) }
    }

    /** Falls back to the key name: a missing string is visible, not an empty label. */
    fun get(key: AppStringKey): String = strings[key] ?: key.toString()
}

// A new group of AppStringKey must be added here too.
private fun allKeys(): List<AppStringKey> =
    AppStringKey.Core.entries + AppStringKey.Tabs.entries + AppStringKey.Placeholder.entries
