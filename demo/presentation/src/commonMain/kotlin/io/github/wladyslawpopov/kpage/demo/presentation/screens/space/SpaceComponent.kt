package io.github.wladyslawpopov.kpage.demo.presentation.screens.space

import io.github.wladyslawpopov.kpage.demo.domain.favorites.FavoritesRepository
import io.github.wladyslawpopov.kpage.demo.domain.settings.SettingsRepository
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceRepository
import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentContext
import io.github.wladyslawpopov.kpage.demo.presentation.component.StateOwner
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.AppNavigator
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.ScreenComponent
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageComponent
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageOwner

interface SpaceComponent : ScreenComponent, StateOwner<SpaceState, SpaceIntent>, PageOwner

internal class DefaultSpaceComponent(
    context: ComponentContext,
    private val navigator: AppNavigator,
    repository: SpaceRepository,
    favorites: FavoritesRepository,
    settings: SettingsRepository,
) : PageComponent<SpaceState, SpaceIntent>(context, SpaceState()), SpaceComponent {

    private val engine = SpaceEngine(
        tasks = this,
        repository = repository,
        favorites = favorites,
        settings = settings,
        states = state,
        getState = { currentState },
        updateState = ::updateState,
        onPageIntent = ::onPageIntent,
    )

    init {
        engine.start()
    }

    override fun onIntent(intent: SpaceIntent) = engine.onIntent(intent)

    override fun onRefresh() = engine.onRefresh()

    override fun onReset() = engine.onReset()
}
