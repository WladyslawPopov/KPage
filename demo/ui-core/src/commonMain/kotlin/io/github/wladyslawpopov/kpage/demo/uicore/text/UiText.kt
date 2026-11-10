package io.github.wladyslawpopov.kpage.demo.uicore.text

/** A text a screen shows: ready data, or a key and values that the platform turns into a string. */
sealed interface UiText {
    data class Plain(val text: String) : UiText

    data class Key(val key: AppStringKey) : UiText

    /** A string key followed by a value, as "From 1960". */
    data class KeyWith(val key: AppStringKey, val text: String) : UiText

    /** A file size: the platform formats it in its own units and language. */
    data class FileSize(val bytes: Long) : UiText

    /** A day (epoch milliseconds): the platform formats it in the user's time zone and language. */
    data class Date(val epochMillis: Long) : UiText

    /** A time of day, hours and minutes (epoch milliseconds), formatted by the platform. */
    data class Time(val epochMillis: Long) : UiText
}
