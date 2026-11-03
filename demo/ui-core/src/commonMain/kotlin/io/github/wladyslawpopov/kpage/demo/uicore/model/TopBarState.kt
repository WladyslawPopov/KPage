package io.github.wladyslawpopov.kpage.demo.uicore.model

import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey

data class TopBarState(
    val title: AppStringKey,
    val canGoBack: Boolean = false,
)
