package io.github.wladyslawpopov.kpage.demo.presentation.screens.favorites

import io.github.wladyslawpopov.kpage.core.paging.data.state.LoadState
import io.github.wladyslawpopov.kpage.demo.domain.favorites.FavoritesRepository
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceImage
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceRepository
import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentTasks
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageEmptyState
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageIntent
import io.github.wladyslawpopov.kpage.demo.presentation.paginator.PaginatorEngine
import io.github.wladyslawpopov.kpage.demo.presentation.paginator.PaginatorIntent
import io.github.wladyslawpopov.kpage.demo.presentation.paginator.PaginatorUiState
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Business scenarios of the Favorites tab. */
internal class FavoritesEngine(
    private val tasks: ComponentTasks,
    private val favorites: FavoritesRepository,
    private val space: SpaceRepository,
    private val getState: () -> FavoritesState,
    private val updateState: ((FavoritesState) -> FavoritesState) -> Unit,
    private val onPageIntent: (PageIntent) -> Unit,
) {
    private val paginator = PaginatorEngine<Unit, SpaceImage, Nothing>(
        getState = { getState().list },
        updateState = ::setList,
        onError = { onPageIntent(PageIntent.OnError(it)) },
    )

    // Removals are applied one after another, in the order they were confirmed.
    private val deleteLock = Mutex()

    fun start() {
        tasks.launchMain {
            paginator.collectPaging(flowOf(Unit)) { _, scope -> favorites.favorites(scope) }
        }
        // The tab lives from the start of the app: a heart set in Space or on the details reaches it here.
        tasks.launchMain {
            favorites.changes.collect { paginator.onIntent(PaginatorIntent.Refresh) }
        }
    }

    fun onIntent(intent: FavoritesIntent) {
        when (intent) {
            is FavoritesIntent.Click -> if (getState().isSelecting) toggle(intent.id)
            is FavoritesIntent.LongClick -> toggle(intent.id)
            FavoritesIntent.ClearSelection -> updateState { it.copy(selected = emptySet()) }
            FavoritesIntent.DeleteSelected -> askToDelete(getState().selected)
            is FavoritesIntent.DeleteOne -> askToDelete(setOf(intent.id))
            FavoritesIntent.ConfirmDelete -> confirmDelete()
            FavoritesIntent.CancelDelete -> updateState { it.copy(pendingDelete = emptySet()) }
        }
    }

    fun onRefresh() = paginator.onIntent(PaginatorIntent.Refresh)

    private fun toggle(id: String) =
        updateState { state ->
            state.copy(selected = if (id in state.selected) state.selected - id else state.selected + id)
        }

    private fun askToDelete(ids: Set<String>) {
        if (ids.isNotEmpty()) updateState { it.copy(pendingDelete = ids) }
    }

    // The database first, then the cache of the Space lists: they show the removed mark without a
    // refresh. The favorites list renews itself on the change of the repository.
    private fun confirmDelete() {
        val ids = getState().pendingDelete
        if (ids.isEmpty()) return
        updateState { it.copy(pendingDelete = emptySet(), selected = it.selected - ids) }
        tasks.launchMain {
            deleteLock.withLock {
                favorites.removeFavorites(ids)
                ids.forEach { id -> space.updateImage(id) { it.copy(isFavorite = false) } }
            }
        }
    }

    private fun setList(list: PaginatorUiState<Unit, SpaceImage, Nothing>) {
        updateState { state ->
            state.copy(
                list = list,
                page = state.page.copy(
                    isLoading = list.loadState is LoadState.INITIAL,
                    empty = if (list.isEmptyResult) {
                        PageEmptyState(
                            title = AppStringKey.Favorites.EMPTY,
                            icon = AppIconKey.FAVORITE_OFF,
                            actionText = null,
                            action = null,
                        )
                    } else {
                        null
                    },
                ),
            )
        }
    }
}
