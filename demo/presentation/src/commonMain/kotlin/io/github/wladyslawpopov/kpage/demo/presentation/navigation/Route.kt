package io.github.wladyslawpopov.kpage.demo.presentation.navigation

import kotlinx.serialization.Serializable

/** Screens of the app. */
@Serializable
sealed interface Route {
    @Serializable data object Space : Route
    @Serializable data object Chat : Route
    @Serializable data object Lab : Route
    @Serializable data object Cache : Route
    @Serializable data object Settings : Route
}
