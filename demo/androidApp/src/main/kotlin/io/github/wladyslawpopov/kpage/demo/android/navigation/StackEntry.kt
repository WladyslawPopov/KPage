package io.github.wladyslawpopov.kpage.demo.android.navigation

import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentContext
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.Route
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.ScreenComponent

/** One screen in a stack: its route, lifecycle and component live while it is in the stack. */
class StackEntry(
    val id: Long,
    val route: Route,
    val context: ComponentContext,
    val component: ScreenComponent,
) {
    /** What the navigator last told the context; avoids repeated calls and drives the debug log. */
    var isShown: Boolean = false
}
