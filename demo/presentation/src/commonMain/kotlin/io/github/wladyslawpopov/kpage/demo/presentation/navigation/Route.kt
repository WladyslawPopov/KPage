package io.github.wladyslawpopov.kpage.demo.presentation.navigation

import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceQuery
import kotlinx.serialization.Serializable

/**
 * Screens of the app. A route is the identity of its screen: opening a route that is already in the
 * stack brings that screen to the top instead of opening a second one.
 */
@Serializable
sealed interface Route {
    @Serializable data object Space : Route

    /** The Space list opened with [query], for example from a field of an image. */
    @Serializable data class SpaceResults(val query: SpaceQuery) : Route

    /** The details of the image with the NASA id [id]. */
    @Serializable data class SpaceDetails(val id: String) : Route

    @Serializable data object Favorites : Route

    @Serializable data object Chat : Route

    /** The chat with the id [chatId]. */
    @Serializable data class ChatRoom(val chatId: Long) : Route

    @Serializable data object Lab : Route
    @Serializable data object Settings : Route
}
