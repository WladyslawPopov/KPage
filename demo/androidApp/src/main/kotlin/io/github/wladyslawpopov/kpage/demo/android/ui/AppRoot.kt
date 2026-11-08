package io.github.wladyslawpopov.kpage.demo.android.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import io.github.wladyslawpopov.kpage.demo.android.navigation.NavigatorViewModel
import io.github.wladyslawpopov.kpage.demo.android.theme.color
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.ui.widgets.TabBar
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey

/**
 * The shell of the app. The pages run under the transparent status bar, under a translucent strip
 * over the system navigation bar and under the tab bar; they pad their content by [LocalPageTopPadding] and [LocalPageBottomPadding]. A page may scroll
 * the tab bar away through [LocalTabBarScrollBehavior].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot() {
    val navigator = viewModel<NavigatorViewModel>().navigator
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = LocalActivity.current

    // The app is visible between ON_RESUME and ON_STOP; a rotation is not leaving.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> navigator.setAppVisible(true)
                Lifecycle.Event.ON_STOP ->
                    if (activity?.isChangingConfigurations != true) navigator.setAppVisible(false)

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val systemNavBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val isImeVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp
    // The keyboard takes the bottom of the screen; the tab bar comes back when it closes.
    val isTabBarVisible = !isImeVisible

    // A stable lambda keeps one behavior, so the pages keep their link to it.
    val canScroll by rememberUpdatedState(isTabBarVisible)
    val tabBarScroll = BottomAppBarDefaults.exitAlwaysScrollBehavior(canScroll = remember { { canScroll } })

    val tab = navigator.selectedTab
    val tabStates = rememberSaveableStateHolder()
    Box(
        Modifier
            .fillMaxSize()
            .background(AppColorKey.Container.BACKGROUND.color())
            .imePadding()
            // The pages pad by LocalPageTopPadding instead.
            .consumeWindowInsets(WindowInsets.statusBars),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom),
            bottomBar = {
                if (isTabBarVisible) {
                    Box(Modifier.padding(bottom = systemNavBar)) {
                        // Shrinks while a page scrolls down; the page padding follows it.
                        BottomAppBar(
                            modifier = Modifier.clipToBounds(),
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp,
                            contentPadding = PaddingValues(0.dp),
                            windowInsets = WindowInsets(0),
                            scrollBehavior = tabBarScroll,
                        ) {
                            TabBar(selected = tab, onSelect = navigator::selectTab)
                        }
                    }
                }
            },
        ) { padding ->
            CompositionLocalProvider(
                LocalPageTopPadding provides statusBar,
                LocalPageBottomPadding provides padding.calculateBottomPadding() + AppDimenKey.Padding.SMALL.dp(),
                LocalTabBarScrollBehavior provides if (isTabBarVisible) tabBarScroll else null,
            ) {
                // Only the selected tab is composed; the others keep their stacks and saved UI state.
                tabStates.SaveableStateProvider(tab.name) {
                    NavDisplay(
                        backStack = navigator.stacks.getValue(tab),
                        onBack = { navigator.back() },
                        entryProvider = { entry ->
                            // A Long key can be saved in a Bundle, unlike the entry itself.
                            NavEntry(entry, contentKey = entry.id) { ScreenContent(entry.component) }
                        },
                    )
                }
            }
        }
        // Drawn over the pages: the content shows through it. The status bar has no strip.
        if (systemNavBar > 0.dp && !isImeVisible) {
            SystemBarStrip(systemNavBar, Modifier.align(Alignment.BottomCenter))
        }
    }
}
