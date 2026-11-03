package io.github.wladyslawpopov.kpage.demo.android.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import io.github.wladyslawpopov.kpage.demo.android.navigation.NavigatorViewModel
import io.github.wladyslawpopov.kpage.demo.android.theme.vector
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.AppTab
import io.github.wladyslawpopov.kpage.demo.shared.strings.asString

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

    val tabStates = rememberSaveableStateHolder()
    Scaffold(
        // Screens draw their own top bars with the status bar inset.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == navigator.selectedTab,
                        onClick = { navigator.selectTab(tab) },
                        icon = { Icon(tab.icon.vector(), contentDescription = null) },
                        label = { Text(tab.title.asString()) },
                    )
                }
            }
        },
    ) { padding ->
        val tab = navigator.selectedTab
        // Only the selected tab is composed; the others keep their stacks and saved UI state.
        tabStates.SaveableStateProvider(tab.name) {
            NavDisplay(
                backStack = navigator.stacks.getValue(tab),
                onBack = { navigator.back() },
                modifier = Modifier.padding(padding),
                entryProvider = { entry ->
                    // A Long key can be saved in a Bundle, unlike the entry itself.
                    NavEntry(entry, contentKey = entry.id) { ScreenContent(entry.component) }
                },
            )
        }
    }
}
