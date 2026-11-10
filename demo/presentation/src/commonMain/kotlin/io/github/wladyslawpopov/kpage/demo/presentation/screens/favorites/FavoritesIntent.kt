package io.github.wladyslawpopov.kpage.demo.presentation.screens.favorites

sealed interface FavoritesIntent {
    /** A tap on an image: opens its details, or chooses it while the screen is in the selection mode. */
    data class Click(val id: String) : FavoritesIntent

    /** A long press on an image: chooses it, or takes the choice back. */
    data class LongClick(val id: String) : FavoritesIntent

    /** Leaves the selection mode. */
    data object ClearSelection : FavoritesIntent

    /** Asks to remove the chosen images. */
    data object DeleteSelected : FavoritesIntent

    /** Asks to remove one image, for example after a swipe. */
    data class DeleteOne(val id: String) : FavoritesIntent

    /** The user agreed to the removal that was asked for. */
    data object ConfirmDelete : FavoritesIntent

    /** The user refused the removal that was asked for. */
    data object CancelDelete : FavoritesIntent
}
