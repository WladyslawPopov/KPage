package io.github.wladyslawpopov.kpage.demo.data.settings

import com.russhwolf.settings.Settings
import io.github.wladyslawpopov.kpage.demo.domain.settings.AppLanguage
import io.github.wladyslawpopov.kpage.demo.domain.settings.SettingsRepository
import io.github.wladyslawpopov.kpage.demo.domain.settings.ThemeMode
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Keeps the choices in the platform's key-value store. The theme and the language are read once when
 * the repository is created (the first read may touch the disk), so the flows have a value from the
 * first frame; writes go to the disk in the background.
 */
internal class SettingsRepositoryImpl(
    private val settings: Settings,
    systemLanguageTag: String,
) : SettingsRepository {

    // An unknown value (a renamed constant) falls back to the default.
    private val mutableThemeMode = MutableStateFlow(
        enumOrNull<ThemeMode>(settings.getStringOrNull(THEME_MODE)) ?: ThemeMode.SYSTEM
    )
    override val themeMode: StateFlow<ThemeMode> = mutableThemeMode.asStateFlow()

    // Nothing is saved until the user chooses: the app follows the system language meanwhile.
    private val mutableLanguage = MutableStateFlow(
        enumOrNull<AppLanguage>(settings.getStringOrNull(LANGUAGE))
            ?: AppLanguage.entries.firstOrNull { it.tag == systemLanguageTag }
            ?: AppLanguage.ENGLISH
    )
    override val language: StateFlow<AppLanguage> = mutableLanguage.asStateFlow()

    override suspend fun spaceLayout(): SpaceLayout = io {
        enumOrNull<SpaceLayout>(settings.getStringOrNull(SPACE_LAYOUT)) ?: SpaceLayout.LIST
    }

    override suspend fun setSpaceLayout(layout: SpaceLayout) {
        io { settings.putString(SPACE_LAYOUT, layout.name) }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        mutableThemeMode.value = mode
        io { settings.putString(THEME_MODE, mode.name) }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        mutableLanguage.value = language
        io { settings.putString(LANGUAGE, language.name) }
    }

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }

    private inline fun <reified E : Enum<E>> enumOrNull(name: String?): E? =
        enumValues<E>().firstOrNull { it.name == name }

    private companion object {
        const val SPACE_LAYOUT = "space_layout"
        const val THEME_MODE = "theme_mode"
        const val LANGUAGE = "language"
    }
}
