package io.github.wladyslawpopov.kpage.demo.android.ui

import androidx.compose.runtime.Composable
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.chat.listing.ChatsScreen
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.chat.room.ChatRoomScreen
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.favorites.FavoritesScreen
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.lab.LabScreen
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.settings.SettingsScreen
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.space.details.SpaceDetailsScreen
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.space.listing.SpaceScreen
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.ScreenComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.listing.ChatsComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room.ChatRoomComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.favorites.FavoritesComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.lab.LabComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.settings.SettingsComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.details.SpaceDetailsComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.SpaceComponent

@Composable
fun ScreenContent(component: ScreenComponent) {
    when (component) {
        is SpaceComponent -> SpaceScreen(component)
        is SpaceDetailsComponent -> SpaceDetailsScreen(component)
        is FavoritesComponent -> FavoritesScreen(component)
        is ChatsComponent -> ChatsScreen(component)
        is ChatRoomComponent -> ChatRoomScreen(component)
        is LabComponent -> LabScreen(component)
        is SettingsComponent -> SettingsScreen(component)
        else -> error("No UI for $component")
    }
}
