package io.github.wladyslawpopov.kpage.demo.android.navigation

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentContext
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.AppNavigator
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.AppTab
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.Route
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.ScreenFactory

/**
 * Navigation of the app: a stack per tab. Creates the components of the routes, destroys them when
 * they leave a stack and decides which one is shown.
 */
class AndroidNavigator(private val factory: ScreenFactory) : AppNavigator {
    private var nextId = 0L
    private var isAppVisible = false

    var selectedTab by mutableStateOf(AppTab.SPACE)
        private set

    val stacks: Map<AppTab, SnapshotStateList<StackEntry>> =
        AppTab.entries.associateWith { tab -> mutableStateListOf(create(tab.root)) }

    override fun open(route: Route) {
        stacks.getValue(selectedTab).add(create(route))
        updateLifecycle()
    }

    override fun back(): Boolean {
        val stack = stacks.getValue(selectedTab)
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex).context.destroy()
        updateLifecycle()
        return true
    }

    fun selectTab(tab: AppTab) {
        selectedTab = tab
        updateLifecycle()
    }

    /** The activity is shown or hidden (not for a configuration change). */
    fun setAppVisible(visible: Boolean) {
        isAppVisible = visible
        updateLifecycle()
    }

    fun destroyAll() = stacks.values.forEach { stack -> stack.forEach { it.context.destroy() } }

    private fun updateLifecycle() {
        stacks.forEach { (tab, stack) ->
            stack.forEachIndexed { index, entry ->
                val shown = isAppVisible && tab == selectedTab && index == stack.lastIndex
                if (entry.isShown == shown) return@forEachIndexed
                entry.isShown = shown
                if (shown) entry.context.show() else entry.context.hide()
                Log.d(TAG, "${entry.route} ${if (shown) "shown" else "hidden"}")
            }
        }
    }

    private fun create(route: Route): StackEntry {
        val context = ComponentContext()
        return StackEntry(nextId++, route, context, factory.create(route, context, this))
    }

    private companion object {
        const val TAG = "KPageDemo"
    }
}
