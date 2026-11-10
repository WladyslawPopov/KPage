package io.github.wladyslawpopov.kpage.demo.presentation.navigation

import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentContext
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.listing.DefaultChatsComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room.DefaultChatRoomComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.favorites.DefaultFavoritesComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.lab.DefaultLabComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.settings.DefaultSettingsComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.details.DefaultSpaceDetailsComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.DefaultSpaceComponent
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/** Creates the component of a route; its dependencies come from DI. */
class ScreenFactory : KoinComponent {
    fun create(route: Route, context: ComponentContext, navigator: AppNavigator): ScreenComponent =
        when (route) {
            Route.Space -> DefaultSpaceComponent(context, navigator, get(), get(), get())
            is Route.SpaceResults ->
                DefaultSpaceComponent(context, navigator, get(), get(), get(), initialQuery = route.query)

            is Route.SpaceDetails -> DefaultSpaceDetailsComponent(context, navigator, route.id, get(), get())
            Route.Favorites -> DefaultFavoritesComponent(context, navigator, get(), get())
            Route.Chat -> DefaultChatsComponent(context, navigator, get())
            is Route.ChatRoom -> DefaultChatRoomComponent(context, navigator, route.chatId, get())
            Route.Lab -> DefaultLabComponent(context, navigator, get())
            Route.Settings -> DefaultSettingsComponent(context, navigator, get())
        }
}
