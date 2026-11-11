package io.github.wladyslawpopov.kpage.demo.presentation.screens.settings

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.github.wladyslawpopov.kpage.demo.domain.cache.CacheRepository
import io.github.wladyslawpopov.kpage.demo.domain.settings.SettingsRepository
import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentContext
import io.github.wladyslawpopov.kpage.demo.presentation.component.StateOwner
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.AppNavigator
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.ScreenComponent
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageComponent
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageOwner

interface SettingsComponent : ScreenComponent, StateOwner<SettingsState, SettingsIntent>, PageOwner

internal class DefaultSettingsComponent(
    context: ComponentContext,
    private val navigator: AppNavigator,
    settings: SettingsRepository,
    cache: CacheRepository,
) : PageComponent<SettingsState, SettingsIntent>(context, SettingsState()), SettingsComponent {

    private val engine = SettingsEngine(
        tasks = this,
        settings = settings,
        cache = cache,
        getState = { currentState },
        updateState = ::updateState,
        onPageIntent = ::onPageIntent,
    )

    init {
        engine.start()
        // The tab is created at the start of the app; the cache has grown by the time it is shown.
        context.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) engine.refreshCache()
        })
    }

    override fun onIntent(intent: SettingsIntent) = engine.onIntent(intent)

    override fun onRefresh() = engine.refreshCache()
}
