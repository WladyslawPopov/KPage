package io.github.wladyslawpopov.kpage.demo.presentation.screens.space.details

import io.github.wladyslawpopov.kpage.demo.domain.favorites.FavoritesRepository
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceQuery
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceRepository
import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentContext
import io.github.wladyslawpopov.kpage.demo.presentation.component.StateOwner
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.AppNavigator
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.Route
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.ScreenComponent
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageComponent
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageOwner

interface SpaceDetailsComponent : ScreenComponent, StateOwner<SpaceDetailsState, SpaceDetailsIntent>, PageOwner

internal class DefaultSpaceDetailsComponent(
    context: ComponentContext,
    private val navigator: AppNavigator,
    id: String,
    repository: SpaceRepository,
    favorites: FavoritesRepository,
) : PageComponent<SpaceDetailsState, SpaceDetailsIntent>(context, SpaceDetailsState()), SpaceDetailsComponent {

    private val engine = SpaceDetailsEngine(
        id = id,
        tasks = this,
        repository = repository,
        favorites = favorites,
        updateState = ::updateState,
        onPageIntent = ::onPageIntent,
    )

    init {
        engine.start()
    }

    override fun onIntent(intent: SpaceDetailsIntent) = when (intent) {
        is SpaceDetailsIntent.SetFavorite -> engine.setFavorite(intent.isFavorite)
        is SpaceDetailsIntent.OpenCenter -> openList(SpaceQuery(center = intent.center))
        is SpaceDetailsIntent.OpenYear -> openList(SpaceQuery(yearStart = intent.year, yearEnd = intent.year))
        is SpaceDetailsIntent.OpenTag -> openList(SpaceQuery(tag = intent.tag))
    }

    override suspend fun onRefresh() = engine.onRefresh()

    private fun openList(query: SpaceQuery) = navigator.open(Route.SpaceResults(query))
}
