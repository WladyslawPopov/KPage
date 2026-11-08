package io.github.wladyslawpopov.kpage.demo.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import io.github.wladyslawpopov.kpage.demo.android.theme.color
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey

/** How far from the top a page keeps its header: under the status bar. */
val LocalPageTopPadding = compositionLocalOf<Dp> {
    error("LocalPageTopPadding is provided by AppRoot")
}

/**
 * How far from the bottom a page keeps its content, toast and badges: above the tab bar, or above the
 * system navigation bar when the tab bar is hidden. Changes while the tab bar slides away.
 */
val LocalPageBottomPadding = compositionLocalOf<Dp> {
    error("LocalPageBottomPadding is provided by AppRoot")
}

/** The scroll behavior of the tab bar; null when the bar is hidden or a page already took it. */
@OptIn(ExperimentalMaterial3Api::class)
val LocalTabBarScrollBehavior = staticCompositionLocalOf<BottomAppBarScrollBehavior?> { null }

/** The background of the bars: the content shows through it. */
@Composable
fun barColor(): Color = AppColorKey.Container.ELEVATED.color().copy(alpha = BAR_ALPHA)

/** The highlight behind a button, the title or the search field of a bar that has no background of its own. */
@Composable
fun barItemColor(): Color = AppColorKey.Container.ELEVATED.color().copy(alpha = ITEM_ALPHA)

/** A translucent strip over the system navigation bar; it takes the taps so they do not reach the content under it. */
@Composable
fun SystemBarStrip(height: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(barColor())
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    )
}

private const val BAR_ALPHA = 0.95f
private const val ITEM_ALPHA = 0.8f
