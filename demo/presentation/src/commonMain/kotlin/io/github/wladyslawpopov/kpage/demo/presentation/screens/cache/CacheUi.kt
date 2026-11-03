package io.github.wladyslawpopov.kpage.demo.presentation.screens.cache

/** UI of the Cache screen; every platform implements it and draws the state it gets. */
interface CacheUi {
    fun renderCache(state: CacheState)
}
