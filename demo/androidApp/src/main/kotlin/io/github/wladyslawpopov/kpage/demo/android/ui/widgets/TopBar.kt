package io.github.wladyslawpopov.kpage.demo.android.ui.widgets

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.style
import io.github.wladyslawpopov.kpage.demo.android.theme.vector
import io.github.wladyslawpopov.kpage.demo.android.ui.barItemColor
import io.github.wladyslawpopov.kpage.demo.shared.strings.asString
import io.github.wladyslawpopov.kpage.demo.uicore.model.TopBarState
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey
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

/**
 * The bar of a plain screen, in the style of the other tabs: no background of its own, the title on a
 * round highlight. The page shows through it; the status bar is covered by the app shell.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    state: TopBarState,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(
                text = state.title.asString(),
                style = AppTypographyKey.TITLE.style(),
                modifier = Modifier
                    .background(barItemColor(), CircleShape)
                    .padding(horizontal = AppDimenKey.Padding.LARGE.dp(), vertical = AppDimenKey.Padding.SMALL.dp()),
            )
        },
        navigationIcon = {
            if (state.canGoBack) {
                BarIconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(start = AppDimenKey.Padding.SMALL.dp()),
                ) {
                    Icon(AppIconKey.BACK.vector(), contentDescription = AppStringKey.Core.BACK.asString())
                }
            }
        },
        actions = {
            actions()
            // With the 4 dp of the bar and the 4 dp of the button, the edge gap matches the 16 dp at the start.
            Spacer(Modifier.width(AppDimenKey.Padding.SMALL.dp()))
        },
        windowInsets = WindowInsets(0),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
    )
}
