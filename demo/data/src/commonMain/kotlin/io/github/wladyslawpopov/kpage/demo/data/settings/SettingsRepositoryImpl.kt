package io.github.wladyslawpopov.kpage.demo.data.settings

import com.russhwolf.settings.Settings
import io.github.wladyslawpopov.kpage.demo.domain.settings.SettingsRepository
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/** Keeps the choices in the platform's key-value store; the first read may touch the disk. */
internal class SettingsRepositoryImpl(private val settings: Settings) : SettingsRepository {

    // An unknown value (a renamed constant) falls back to the default.
    override suspend fun spaceLayout(): SpaceLayout = io {
        val name = settings.getStringOrNull(SPACE_LAYOUT)
        SpaceLayout.entries.firstOrNull { it.name == name } ?: SpaceLayout.LIST
    }

    override suspend fun setSpaceLayout(layout: SpaceLayout) {
        io { settings.putString(SPACE_LAYOUT, layout.name) }
    }

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }

    private companion object {
        const val SPACE_LAYOUT = "space_layout"
    }
}
