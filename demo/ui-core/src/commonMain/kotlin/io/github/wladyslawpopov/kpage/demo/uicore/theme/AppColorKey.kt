package io.github.wladyslawpopov.kpage.demo.uicore.theme

/** Colors by purpose; every app maps them to its own palette. */
sealed interface AppColorKey {
    enum class Text : AppColorKey { PRIMARY, SECONDARY, MUTED, ACCENT, ON_ACCENT, THEME_1, THEME_2 }

    enum class Container :
        AppColorKey { BACKGROUND, PRIMARY, SECONDARY, ACCENT, ELEVATED, THEME_1, THEME_2 }

    enum class Status : AppColorKey { SUCCESS, WARNING, FAIL, NONE }

    enum class Border : AppColorKey { DEFAULT, ACCENT, FOCUSED }

    enum class Icon : AppColorKey { PRIMARY, SECONDARY, ACCENT, ON_ACCENT }

    enum class Overlay : AppColorKey { SCRIM, DISABLED }
}
