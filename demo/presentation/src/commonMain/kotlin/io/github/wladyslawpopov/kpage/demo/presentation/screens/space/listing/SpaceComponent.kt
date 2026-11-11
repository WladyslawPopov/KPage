package io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing

import io.github.wladyslawpopov.kpage.demo.domain.cache.CacheRepository
import io.github.wladyslawpopov.kpage.demo.domain.favorites.FavoritesRepository
import io.github.wladyslawpopov.kpage.demo.domain.settings.SettingsRepository
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceQuery
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceRepository
import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentContext
import io.github.wladyslawpopov.kpage.demo.presentation.component.StateOwner
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.AppNavigator
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.Route
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.ScreenComponent
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageComponent
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageOwner
import io.github.wladyslawpopov.kpage.demo.presentation.paginator.PaginatorUiState
import io.github.wladyslawpopov.kpage.demo.uicore.model.TopBarState
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey

interface SpaceComponent : ScreenComponent, StateOwner<SpaceState, SpaceIntent>, PageOwner

/**
 * The Space list: the root of the tab, or a list opened with [initialQuery] above it (then it has
 * the back button).
 */
internal class DefaultSpaceComponent(
    context: ComponentContext,
    private val navigator: AppNavigator,
    repository: SpaceRepository,
    favorites: FavoritesRepository,
    settings: SettingsRepository,
    cache: CacheRepository,
    initialQuery: SpaceQuery? = null,
) : PageComponent<SpaceState, SpaceIntent>(context, initialState(initialQuery)), SpaceComponent {

    private val engine = SpaceEngine(
        tasks = this,
        initialQuery = initialQuery ?: SpaceQuery(),
        repository = repository,
        favorites = favorites,
        settings = settings,
        cache = cache,
        states = state,
        getState = { currentState },
        updateState = ::updateState,
        onPageIntent = ::onPageIntent,
    )

    init {
        engine.start()
    }

    override fun onIntent(intent: SpaceIntent) = when (intent) {
        is SpaceIntent.OpenImage -> navigator.open(Route.SpaceDetails(intent.id))
        else -> engine.onIntent(intent)
    }

    override fun onRefresh() = engine.onRefresh()

    override fun onReset() = engine.onReset()

    private companion object {
        fun initialState(query: SpaceQuery?): SpaceState =
            if (query == null) {
                SpaceState()
            } else {
                SpaceState(
                    topBar = TopBarState(AppStringKey.Tabs.SPACE, canGoBack = true),
                    searchText = query.text,
                    list = PaginatorUiState(query),
                )
            }
    }
}
