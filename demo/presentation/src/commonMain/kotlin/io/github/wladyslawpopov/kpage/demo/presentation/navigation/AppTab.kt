package io.github.wladyslawpopov.kpage.demo.presentation.navigation

import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey

/** Tabs of the bottom menu; each has its own stack starting at [root]. */
enum class AppTab(val title: AppStringKey, val icon: AppIconKey, val root: Route) {
    SPACE(AppStringKey.Tabs.SPACE, AppIconKey.SPACE, Route.Space),
    FAVORITES(AppStringKey.Tabs.FAVORITES, AppIconKey.FAVORITE, Route.Favorites),
    CHAT(AppStringKey.Tabs.CHAT, AppIconKey.CHAT, Route.Chat),
    LAB(AppStringKey.Tabs.LAB, AppIconKey.LAB, Route.Lab),
    SETTINGS(AppStringKey.Tabs.SETTINGS, AppIconKey.SETTINGS, Route.Settings),
}
