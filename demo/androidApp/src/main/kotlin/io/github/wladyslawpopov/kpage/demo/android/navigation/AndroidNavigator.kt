package io.github.wladyslawpopov.kpage.demo.android.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.AppNavigator
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.AppTab
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.NavigationSnapshot
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.Route
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.ScreenFactory

/**
 * Navigation of the app: the selected tab and app visibility. The components live in
 * [ComponentHolder], which is told about every change. [onChanged] receives the new snapshot
 * after every navigation change.
 */
class AndroidNavigator(
    private val factory: ScreenFactory,
    saved: NavigationSnapshot? = null,
    private val onChanged: (NavigationSnapshot) -> Unit = {},
) : AppNavigator {
    private val holder = ComponentHolder(saved?.paths.orEmpty()) { route, context ->
        factory.create(route, context, this)
    }
    private var isAppVisible = false

    var selectedTab by mutableStateOf(saved?.selectedTab ?: AppTab.SPACE)
        private set

    val stacks get() = holder.stacks

    override fun open(route: Route) {
        holder.push(selectedTab, route)
        changed()
    }

    override fun back(): Boolean {
        if (!holder.pop(selectedTab)) return false
        changed()
        return true
    }

    fun selectTab(tab: AppTab) {
        selectedTab = tab
        changed()
    }

    /** The activity is shown or hidden (not for a configuration change). */
    fun setAppVisible(visible: Boolean) {
        isAppVisible = visible
        updateLifecycle()
    }

    fun destroyAll() = holder.destroyAll()

    private fun changed() {
        updateLifecycle()
        onChanged(NavigationSnapshot(selectedTab, holder.paths()))
    }

    private fun updateLifecycle() = holder.updateLifecycle(selectedTab, isAppVisible)
}
