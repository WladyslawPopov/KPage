package io.github.wladyslawpopov.kpage.demo.android.theme

import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import io.github.wladyslawpopov.kpage.demo.domain.settings.AppLanguage
import java.util.Locale

/**
 * Makes the strings of the app follow [language] instead of the system language, without recreating
 * the activity: the resources read the locale of the composition's configuration.
 */
@Composable
fun AppLanguageProvider(language: AppLanguage, content: @Composable () -> Unit) {
    val base = LocalConfiguration.current
    val configuration = remember(base, language) {
        val locale = Locale.forLanguageTag(language.tag)
        Locale.setDefault(locale)
        Configuration(base).apply { setLocales(LocaleList(locale)) }
    }
    CompositionLocalProvider(LocalConfiguration provides configuration, content = content)
}
