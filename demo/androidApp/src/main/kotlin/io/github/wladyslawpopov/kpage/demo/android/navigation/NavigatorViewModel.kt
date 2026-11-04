package io.github.wladyslawpopov.kpage.demo.android.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.NavigationSnapshotCodec
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/**
 * Keeps the navigator, and with it every component, through configuration changes. The navigation
 * snapshot also survives process death through [SavedStateHandle].
 */
class NavigatorViewModel(savedState: SavedStateHandle) : ViewModel(), KoinComponent {
    val navigator = AndroidNavigator(
        factory = get(),
        saved = savedState.get<String>(KEY)?.let(NavigationSnapshotCodec::decode),
        onChanged = { savedState[KEY] = NavigationSnapshotCodec.encode(it) },
    )

    override fun onCleared() = navigator.destroyAll()

    private companion object {
        const val KEY = "navigation"
    }
}
