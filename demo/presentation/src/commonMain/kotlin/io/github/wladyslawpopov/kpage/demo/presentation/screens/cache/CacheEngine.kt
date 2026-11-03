package io.github.wladyslawpopov.kpage.demo.presentation.screens.cache

import io.github.wladyslawpopov.kpage.demo.domain.cache.CacheRepository
import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentTasks
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageIntent

/** Business scenarios of the Cache tab. */
internal class CacheEngine(
    private val tasks: ComponentTasks,
    private val repository: CacheRepository,
    private val getState: () -> CacheState,
    private val updateState: ((CacheState) -> CacheState) -> Unit,
    private val onPageIntent: (PageIntent) -> Unit,
) {
    fun start() = Unit

    fun onIntent(intent: CacheIntent) = Unit
}
