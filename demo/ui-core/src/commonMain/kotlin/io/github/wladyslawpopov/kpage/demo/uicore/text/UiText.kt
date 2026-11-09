package io.github.wladyslawpopov.kpage.demo.uicore.text

/** A text a screen shows: ready data, or a key and values that the platform turns into a string. */
sealed interface UiText {
    data class Plain(val text: String) : UiText

    data class Key(val key: AppStringKey) : UiText

    /** A string key followed by a value, as "From 1960". */
    data class KeyWith(val key: AppStringKey, val text: String) : UiText

    /** A file size: the platform formats it in its own units and language. */
    data class FileSize(val bytes: Long) : UiText
}
