package io.github.wladyslawpopov.kpage.demo.android

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import coil3.compose.setSingletonImageLoaderFactory
import io.github.wladyslawpopov.kpage.demo.android.theme.AppLanguageProvider
import io.github.wladyslawpopov.kpage.demo.android.theme.DemoTheme
import io.github.wladyslawpopov.kpage.demo.android.ui.AppRoot
import io.github.wladyslawpopov.kpage.demo.android.ui.widgets.createImageLoader
import io.github.wladyslawpopov.kpage.demo.domain.settings.SettingsRepository
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val settings: SettingsRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The bottom bar of the app paints the area of the navigation buttons itself.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
        setContent {
            setSingletonImageLoaderFactory(::createImageLoader)
            // The choices of the Settings tab: the app follows the repository's flows.
            val themeMode by settings.themeMode.collectAsState()
            val language by settings.language.collectAsState()
            AppLanguageProvider(language) {
                DemoTheme(themeMode) {
                    // Test tags are shown to UI tools (Maestro, UiAutomator) as resource ids.
                    Box(Modifier.semantics { testTagsAsResourceId = true }) { AppRoot() }
                }
            }
        }
    }
}
