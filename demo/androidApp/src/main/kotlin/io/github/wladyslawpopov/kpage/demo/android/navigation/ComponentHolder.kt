package io.github.wladyslawpopov.kpage.demo.android.navigation

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentContext
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.AppTab
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.Route
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.ScreenComponent

/**
 * Creates, keeps and destroys the components of the app: a stack of entries per tab. Moves their
 * lifecycles by one rule: only the top screen of the selected tab is shown, and only while the app is.
 */
class ComponentHolder(
    initialPaths: Map<AppTab, List<Route>> = emptyMap(),
    private val createComponent: (Route, ComponentContext) -> ScreenComponent,
) {
    private var nextId = 0L // Declared before [stacks]: its initializer creates the entries.

    val stacks: Map<AppTab, SnapshotStateList<StackEntry>> =
        AppTab.entries.associateWith { tab ->
            mutableStateListOf(create(tab.root)).apply {
                initialPaths[tab].orEmpty().forEach { add(create(it)) }
            }
        }

    /** The routes of every tab above its root. */
    fun paths(): Map<AppTab, List<Route>> =
        stacks.mapValues { (_, stack) -> stack.drop(1).map { it.route } }

    fun push(tab: AppTab, route: Route) {
        stacks.getValue(tab).add(create(route))
    }

    /** Removes and destroys the top entry of [tab]; false when only the root is left. */
    fun pop(tab: AppTab): Boolean {
        val stack = stacks.getValue(tab)
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex).context.destroy()
        return true
    }

    fun updateLifecycle(selectedTab: AppTab, isAppVisible: Boolean) {
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

    fun destroyAll() = stacks.values.forEach { stack -> stack.forEach { it.context.destroy() } }

    private fun create(route: Route): StackEntry {
        val context = ComponentContext()
        return StackEntry(nextId++, route, context, createComponent(route, context))
    }

    private companion object {
        const val TAG = "KPageDemo"
    }
}
