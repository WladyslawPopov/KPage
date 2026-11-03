package io.github.wladyslawpopov.kpage.demo.presentation.component

import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey

enum class ErrorKind { NO_INTERNET, NETWORK, SERVER, UNKNOWN }

/** An error ready to be shown: what happened ([kind]), our [title] and the server's [message], if any. */
data class ScreenError(
    val kind: ErrorKind = ErrorKind.UNKNOWN,
    val title: AppStringKey = AppStringKey.Core.OOPS_TITLE,
    /** Text of the server, shown instead of [title] when present. */
    val message: String? = null,
    /** Technical code shown next to the text, if the server gave one. */
    val code: String? = null,
)
