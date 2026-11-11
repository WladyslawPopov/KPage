package io.github.wladyslawpopov.kpage.demo.domain.settings

import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceLayout
import kotlinx.coroutines.flow.StateFlow

/**
 * The user's choices, kept on the device between launches. The theme and the language are flows:
 * the root of each platform follows them, so a change shows at once.
 */
interface SettingsRepository {
    val themeMode: StateFlow<ThemeMode>

    /** The saved choice; before the first one, the system language if the app has it, else English. */
    val language: StateFlow<AppLanguage>

    suspend fun spaceLayout(): SpaceLayout

    suspend fun setSpaceLayout(layout: SpaceLayout)

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setLanguage(language: AppLanguage)
}
