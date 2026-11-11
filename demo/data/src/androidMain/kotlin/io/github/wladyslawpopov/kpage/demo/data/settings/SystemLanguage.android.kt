package io.github.wladyslawpopov.kpage.demo.data.settings

import java.util.Locale

internal actual fun systemLanguageTag(): String = Locale.getDefault().language
