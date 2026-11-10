package io.github.wladyslawpopov.kpage.demo.android.theme

import android.text.format.DateFormat
import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import io.github.wladyslawpopov.kpage.demo.shared.strings.asString
import io.github.wladyslawpopov.kpage.demo.uicore.text.UiText
import java.util.Date

/** The text of a [UiText]: strings from the resources, file sizes, dates and times as the system formats them. */
@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Plain -> text
    is UiText.Key -> key.asString()
    is UiText.KeyWith -> "${key.asString()} $text"
    is UiText.FileSize -> Formatter.formatShortFileSize(LocalContext.current, bytes)
    is UiText.Date -> DateFormat.getMediumDateFormat(LocalContext.current).format(Date(epochMillis))
    is UiText.Time -> DateFormat.getTimeFormat(LocalContext.current).format(Date(epochMillis))
}
