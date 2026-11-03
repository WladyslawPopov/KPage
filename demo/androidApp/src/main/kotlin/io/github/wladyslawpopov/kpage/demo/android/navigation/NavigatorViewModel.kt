package io.github.wladyslawpopov.kpage.demo.android.navigation

import androidx.lifecycle.ViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/** Keeps the navigator, and with it every component, through configuration changes. */
class NavigatorViewModel : ViewModel(), KoinComponent {
    val navigator = AndroidNavigator(get())

    override fun onCleared() = navigator.destroyAll()
}
