package io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing

import io.github.wladyslawpopov.kpage.core.paging.data.state.LoadState
import io.github.wladyslawpopov.kpage.demo.domain.favorites.FavoritesRepository
import io.github.wladyslawpopov.kpage.demo.domain.settings.SettingsRepository
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceImage
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceLayout
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceQuery
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceRepository
import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentTasks
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageEmptyState
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageIntent
import io.github.wladyslawpopov.kpage.demo.presentation.paginator.PaginatorEngine
import io.github.wladyslawpopov.kpage.demo.presentation.paginator.PaginatorIntent
import io.github.wladyslawpopov.kpage.demo.presentation.paginator.PaginatorUiState
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.milliseconds

/**
 * Business scenarios of the Space list.
 *
 * @param initialQuery Where the list starts and where a reset brings it back: no filters for the tab,
 * a filter for a list opened from an image.
 */
internal class SpaceEngine(
    private val tasks: ComponentTasks,
    private val initialQuery: SpaceQuery,
    private val repository: SpaceRepository,
    private val favorites: FavoritesRepository,
    private val settings: SettingsRepository,
    private val states: Flow<SpaceState>,
    private val getState: () -> SpaceState,
    private val updateState: ((SpaceState) -> SpaceState) -> Unit,
    private val onPageIntent: (PageIntent) -> Unit,
) {
    private val paginator = PaginatorEngine<SpaceQuery, SpaceImage, Nothing>(
        getState = { getState().list },
        updateState = ::setList,
        onQueryChanged = { onPageIntent(PageIntent.ClearError) },
        onError = { onPageIntent(PageIntent.OnError(it)) },
    )

    // Hearts are applied one after another, in the order of the taps.
    private val favoriteLock = Mutex()

    fun start() {
        tasks.launchMain {
            paginator.collectPaging(states.map { it.list.query }) { query, scope ->
                repository.images(query, scope)
            }
        }
        tasks.launchMain { followSearchText() }
        tasks.launchMain {
            val layout = settings.spaceLayout()
            updateState { it.copy(layout = layout) }
        }
    }

    fun onIntent(intent: SpaceIntent) {
        when (intent) {
            is SpaceIntent.SearchChanged -> updateState { it.copy(searchText = intent.text) }
            is SpaceIntent.SetFilters -> setQuery(
                getState().list.query.copy(
                    center = intent.center,
                    yearStart = intent.yearStart,
                    yearEnd = intent.yearEnd,
                    tag = intent.tag,
                )
            )

            SpaceIntent.ToggleLayout -> toggleLayout()

            is SpaceIntent.SetFavorite -> setFavorite(intent)

            // Navigation belongs to the component.
            is SpaceIntent.OpenImage -> Unit
        }
    }

    fun onRefresh() = paginator.onIntent(PaginatorIntent.Refresh)

    /** Back to the start: the text and the filters the list was opened with. */
    fun onReset() {
        updateState { it.copy(searchText = initialQuery.text) }
        setQuery(initialQuery)
    }

    private fun setQuery(query: SpaceQuery) = paginator.onIntent(PaginatorIntent.SetQuery(query))

    /**
     * The list follows the text a moment after the last keystroke. Only the text is watched, so other
     * changes of the state do not restart the wait.
     */
    @OptIn(FlowPreview::class)
    private suspend fun followSearchText() {
        states.map { it.searchText }
            .distinctUntilChanged()
            .debounce(SEARCH_DELAY)
            .map { it.trim() }
            .distinctUntilChanged()
            .collect { text -> setQuery(getState().list.query.copy(text = text)) }
    }

    // The screen switches at once; the choice is saved for the next launch.
    private fun toggleLayout() {
        val layout = if (getState().layout == SpaceLayout.LIST) SpaceLayout.GRID else SpaceLayout.LIST
        updateState { it.copy(layout = layout) }
        tasks.launchMain { settings.setSpaceLayout(layout) }
    }

    // The database first, then the cache of the lists: they all show the new mark without a refresh.
    private fun setFavorite(intent: SpaceIntent.SetFavorite) {
        tasks.launchMain {
            favoriteLock.withLock {
                favorites.setFavorite(intent.id, intent.isFavorite)
                paginator.updateItem(intent.id) { it.copy(isFavorite = intent.isFavorite) }
            }
        }
    }

    // The page shows what the list has no content for: a loader before the first data, "nothing found"
    // with a way back when a search or a filter was changed.
    private fun setList(list: PaginatorUiState<SpaceQuery, SpaceImage, Nothing>) {
        updateState { state ->
            state.copy(
                list = list,
                page = state.page.copy(
                    isLoading = list.loadState is LoadState.INITIAL,
                    empty = when {
                        !list.isEmptyResult -> null
                        list.query != initialQuery -> PageEmptyState(
                            actionText = AppStringKey.Core.RESET,
                            action = PageIntent.Reset,
                        )

                        else -> PageEmptyState()
                    },
                ),
            )
        }
    }

    private companion object {
        val SEARCH_DELAY = 400.milliseconds
    }
}
