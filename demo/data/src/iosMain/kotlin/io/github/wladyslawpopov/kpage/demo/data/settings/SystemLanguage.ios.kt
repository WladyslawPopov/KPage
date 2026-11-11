package io.github.wladyslawpopov.kpage.demo.data.settings

import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

// Entries look like "uk-UA" or "zh-Hans-CN": the language code is the part before the first dash.
internal actual fun systemLanguageTag(): String =
    (NSLocale.preferredLanguages.firstOrNull() as? String)?.substringBefore('-').orEmpty()
