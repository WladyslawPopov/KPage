package io.github.wladyslawpopov.kpage.demo.android.ui.widgets

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.wladyslawpopov.kpage.demo.android.theme.style
import io.github.wladyslawpopov.kpage.demo.android.theme.vector
import io.github.wladyslawpopov.kpage.demo.android.ui.barColor
import io.github.wladyslawpopov.kpage.demo.shared.strings.asString
import io.github.wladyslawpopov.kpage.demo.uicore.model.TopBarState
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppTypographyKey

/**
 * The back button of a screen: goes back as the system back does, so the handlers of the screen (an
 * open search field) and of the navigation run in the same order.
 */
@Composable
fun rememberBackAction(): () -> Unit {
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    return remember(dispatcher) { { dispatcher?.onBackPressed() } }
}

/** Translucent, so the page shows through it; the status bar is covered by the app shell. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    state: TopBarState,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(state.title.asString(), style = AppTypographyKey.TITLE.style()) },
        navigationIcon = {
            if (state.canGoBack) {
                IconButton(onClick = onBack) {
                    Icon(AppIconKey.BACK.vector(), contentDescription = AppStringKey.Core.BACK.asString())
                }
            }
        },
        actions = actions,
        windowInsets = WindowInsets(0),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = barColor(),
            scrolledContainerColor = barColor(),
        ),
    )
}
