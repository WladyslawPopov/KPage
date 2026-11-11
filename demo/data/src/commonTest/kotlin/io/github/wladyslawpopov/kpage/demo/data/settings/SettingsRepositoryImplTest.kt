package io.github.wladyslawpopov.kpage.demo.data.settings

import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import io.github.wladyslawpopov.kpage.demo.domain.settings.AppLanguage
import io.github.wladyslawpopov.kpage.demo.domain.settings.ThemeMode
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceLayout
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsRepositoryImplTest {

    private fun repository(settings: Settings = MapSettings(), systemTag: String = "en") =
        SettingsRepositoryImpl(settings, systemTag)

    @Test
    fun nothing_saved_follows_a_supported_system_language() {
        assertEquals(AppLanguage.UKRAINIAN, repository(systemTag = "uk").language.value)
    }

    @Test
    fun nothing_saved_and_unsupported_system_language_falls_back_to_english() {
        assertEquals(AppLanguage.ENGLISH, repository(systemTag = "de").language.value)
    }

    @Test
    fun saved_language_wins_over_the_system_one() {
        val settings = MapSettings("language" to AppLanguage.POLISH.name)

        assertEquals(AppLanguage.POLISH, repository(settings, systemTag = "ru").language.value)
    }

    @Test
    fun unknown_saved_values_fall_back_to_defaults() = runTest {
        val settings = MapSettings(
            "theme_mode" to "NEON",
            "language" to "KLINGON",
            "space_layout" to "CUBES",
        )
        val repository = repository(settings, systemTag = "de")

        assertEquals(ThemeMode.SYSTEM, repository.themeMode.value)
        assertEquals(AppLanguage.ENGLISH, repository.language.value)
        assertEquals(SpaceLayout.LIST, repository.spaceLayout())
    }

    @Test
    fun choices_update_the_flows_and_survive_a_restart() = runTest {
        val settings = MapSettings()
        val repository = repository(settings)

        repository.setThemeMode(ThemeMode.DARK)
        repository.setLanguage(AppLanguage.RUSSIAN)
        repository.setSpaceLayout(SpaceLayout.entries.last())

        assertEquals(ThemeMode.DARK, repository.themeMode.value)
        assertEquals(AppLanguage.RUSSIAN, repository.language.value)

        val restarted = repository(settings)
        assertEquals(ThemeMode.DARK, restarted.themeMode.value)
        assertEquals(AppLanguage.RUSSIAN, restarted.language.value)
        assertEquals(SpaceLayout.entries.last(), restarted.spaceLayout())
    }
}
