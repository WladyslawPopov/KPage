package io.github.wladyslawpopov.kpage.demo.presentation.screens.favorites

import io.github.wladyslawpopov.kpage.demo.domain.favorites.FavoritesRepository
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceRepository
import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentContext
import io.github.wladyslawpopov.kpage.demo.presentation.component.StateOwner
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.AppNavigator
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.Route
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.ScreenComponent
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageComponent
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageOwner

interface FavoritesComponent : ScreenComponent, StateOwner<FavoritesState, FavoritesIntent>, PageOwner

/** The Favorites list: always the root of its tab. */
internal class DefaultFavoritesComponent(
    context: ComponentContext,
    private val navigator: AppNavigator,
    favorites: FavoritesRepository,
    space: SpaceRepository,
) : PageComponent<FavoritesState, FavoritesIntent>(context, FavoritesState()), FavoritesComponent {

    private val engine = FavoritesEngine(
        tasks = this,
        favorites = favorites,
        space = space,
        getState = { currentState },
        updateState = ::updateState,
        onPageIntent = ::onPageIntent,
    )

    init {
        engine.start()
    }

    override fun onIntent(intent: FavoritesIntent) {
        // Navigation belongs to the component; a tap in the selection mode only chooses.
        if (intent is FavoritesIntent.Click && !currentState.isSelecting) {
            navigator.open(Route.SpaceDetails(intent.id))
        } else {
            engine.onIntent(intent)
        }
    }

    override fun onRefresh() = engine.onRefresh()
}
