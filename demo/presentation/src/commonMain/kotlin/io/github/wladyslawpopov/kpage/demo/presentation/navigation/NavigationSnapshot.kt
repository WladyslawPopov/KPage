package io.github.wladyslawpopov.kpage.demo.presentation.navigation

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** What survives process death: the selected tab and the routes above the root of every tab. */
@Serializable
data class NavigationSnapshot(val selectedTab: AppTab, val paths: Map<AppTab, List<Route>>)

/** Text form of [NavigationSnapshot] for platform storage. */
object NavigationSnapshotCodec {
    fun encode(snapshot: NavigationSnapshot): String = Json.encodeToString(snapshot)

    /** Null for anything that is not a valid snapshot (empty, corrupted, an unknown route). */
    fun decode(text: String): NavigationSnapshot? =
        try {
            Json.decodeFromString<NavigationSnapshot>(text)
        } catch (_: IllegalArgumentException) { // SerializationException is its subclass.
            null
        }
}
