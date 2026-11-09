package io.github.wladyslawpopov.kpage.demo.android.ui

import androidx.compose.runtime.Composable
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.CacheScreen
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.ChatScreen
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.LabScreen
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.SettingsScreen
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.space.details.SpaceDetailsScreen
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.space.listing.SpaceScreen
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.ScreenComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.cache.CacheComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.ChatComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.lab.LabComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.settings.SettingsComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.details.SpaceDetailsComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.SpaceComponent

@Composable
fun ScreenContent(component: ScreenComponent) {
    when (component) {
        is SpaceComponent -> SpaceScreen(component)
        is SpaceDetailsComponent -> SpaceDetailsScreen(component)
        is ChatComponent -> ChatScreen(component)
        is LabComponent -> LabScreen(component)
        is CacheComponent -> CacheScreen(component)
        is SettingsComponent -> SettingsScreen(component)
        else -> error("No UI for $component")
    }
}
