package io.github.wladyslawpopov.kpage.demo.presentation.navigation

/** What the shared code may ask; how and where a route is shown is decided by the platform. */
interface AppNavigator {
    fun open(route: Route)

    /** Closes the current screen; false when there is nothing to close (the platform decides what next). */
    fun back(): Boolean
}
