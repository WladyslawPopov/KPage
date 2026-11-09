package io.github.wladyslawpopov.kpage.demo.android.theme

import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import io.github.wladyslawpopov.kpage.demo.shared.strings.asString
import io.github.wladyslawpopov.kpage.demo.uicore.text.UiText

/** The text of a [UiText]: strings from the resources, file sizes in the units of the system. */
@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Plain -> text
    is UiText.Key -> key.asString()
    is UiText.KeyWith -> "${key.asString()} $text"
    is UiText.FileSize -> Formatter.formatShortFileSize(LocalContext.current, bytes)
}
