package io.github.wladyslawpopov.kpage.demo.presentation.screens.space.details

import io.github.wladyslawpopov.kpage.demo.domain.favorites.FavoritesRepository
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceRepository
import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentTasks
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageEmptyState
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageIntent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.details.mapper.toDisplay
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Business scenarios of the details of one image. */
internal class SpaceDetailsEngine(
    private val id: String,
    private val tasks: ComponentTasks,
    private val repository: SpaceRepository,
    private val favorites: FavoritesRepository,
    private val updateState: ((SpaceDetailsState) -> SpaceDetailsState) -> Unit,
    private val onPageIntent: (PageIntent) -> Unit,
) {
    private var loadJob: Job? = null

    // Hearts are applied one after another, in the order of the taps.
    private val favoriteLock = Mutex()

    fun start() = load()

    fun onRefresh() = load()

    fun setFavorite(isFavorite: Boolean) {
        tasks.launchMain {
            favoriteLock.withLock {
                favorites.setFavorite(id, isFavorite)
                // The lists keep their own copy of the image: they show the new mark without a refresh.
                repository.updateImage(id) { it.copy(isFavorite = isFavorite) }
                updateState { state -> state.copy(display = state.display?.copy(isFavorite = isFavorite)) }
            }
        }
    }

    // The image stays on screen while it loads again; a failure leaves it and shows the error.
    private fun load() {
        loadJob?.cancel()
        onPageIntent(PageIntent.SetLoading(true))
        loadJob = tasks.launchMain(
            onError = { error ->
                onPageIntent(PageIntent.SetLoading(false))
                onPageIntent(PageIntent.OnError(error))
            },
        ) {
            val image = repository.details(id)
            updateState { state ->
                state.copy(
                    display = image?.toDisplay(),
                    page = state.page.copy(
                        isLoading = false,
                        error = null,
                        empty = if (image == null) PageEmptyState() else null,
                    ),
                )
            }
        }
    }
}
