package io.github.wladyslawpopov.kpage.demo.android.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import io.github.wladyslawpopov.kpage.demo.android.ui.page.PageLayout
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.settings.parts.CacheSection
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.settings.parts.ClearCacheDialog
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.settings.parts.OptionsSection
import io.github.wladyslawpopov.kpage.demo.android.ui.widgets.TopBar
import io.github.wladyslawpopov.kpage.demo.presentation.screens.settings.SettingsComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.settings.SettingsIntent
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey

@Composable
fun SettingsScreen(component: SettingsComponent) {
    val state by component.state.collectAsState()

    if (state.confirmClear) {
        ClearCacheDialog(
            onConfirm = { component.onIntent(SettingsIntent.ConfirmClearCache) },
            onCancel = { component.onIntent(SettingsIntent.CancelClearCache) },
        )
    }
    PageLayout(
        page = state.page,
        onPageIntent = component::onPageIntent,
        // The tab root: the back button is never shown.
        header = { TopBar(state.topBar, onBack = {}) },
    ) { padding ->
        LazyColumn(Modifier.fillMaxWidth(), contentPadding = padding) {
            item {
                OptionsSection(
                    title = AppStringKey.Settings.THEME,
                    options = state.themes,
                    onSelect = { component.onIntent(SettingsIntent.SetTheme(it)) },
                )
            }
            item {
                OptionsSection(
                    title = AppStringKey.Settings.LANGUAGE,
                    options = state.languages,
                    onSelect = { component.onIntent(SettingsIntent.SetLanguage(it)) },
                )
            }
            item {
                CacheSection(
                    lists = state.cacheLists,
                    size = state.cacheSize,
                    onClear = { component.onIntent(SettingsIntent.ClearCache) },
                )
            }
        }
    }
}
