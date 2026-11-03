package io.github.wladyslawpopov.kpage.demo.presentation.screens.cache

import io.github.wladyslawpopov.kpage.demo.domain.cache.CacheRepository
import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentContext
import io.github.wladyslawpopov.kpage.demo.presentation.component.StateOwner
import io.github.wladyslawpopov.kpage.demo.presentation.component.UiBinding
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.AppNavigator
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.ScreenComponent
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageComponent
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageOwner

interface CacheComponent : ScreenComponent, StateOwner<CacheState, CacheIntent>, PageOwner {
    /** Subscribes [ui] to the state of this screen. */
    fun bindCache(ui: CacheUi): UiBinding
}

internal class DefaultCacheComponent(
    context: ComponentContext,
    private val navigator: AppNavigator,
    repository: CacheRepository,
) : PageComponent<CacheState, CacheIntent>(context, CacheState()), CacheComponent {

    private val engine = CacheEngine(
        tasks = this,
        repository = repository,
        getState = { currentState },
        updateState = ::updateState,
        onPageIntent = ::onPageIntent,
    )

    init {
        engine.start()
    }

    override fun onIntent(intent: CacheIntent) = engine.onIntent(intent)

    override fun bindCache(ui: CacheUi): UiBinding = bindState(ui::renderCache)
}
