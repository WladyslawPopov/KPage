package io.github.wladyslawpopov.kpage.demo.shared.state

import io.github.wladyslawpopov.kpage.demo.domain.settings.AppLanguage
import io.github.wladyslawpopov.kpage.demo.domain.settings.SettingsRepository
import io.github.wladyslawpopov.kpage.demo.domain.settings.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The theme and the language of the app for the Swift root. They belong to the app, not to a
 * screen, so there is no component to bind to: [observe] runs until [UiBinding.unbind].
 * The current values are readable at once, so the first frame is already right.
 */
class AppSettingsObserver(private val settings: SettingsRepository) {
    val themeMode: ThemeMode get() = settings.themeMode.value
    val language: AppLanguage get() = settings.language.value

    /** Calls [onChange] on Main whenever the theme or the language changes; read the new values then. */
    fun observe(onChange: () -> Unit): UiBinding {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        scope.launch { settings.themeMode.collect { onChange() } }
        scope.launch { settings.language.collect { onChange() } }
        return UiBinding { scope.cancel() }
    }
}
