package io.github.wladyslawpopov.kpage.demo.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.wladyslawpopov.kpage.demo.shared.strings.asString
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "KPage Demo") {
        MaterialTheme {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    AppStringKey.Placeholder.COMING_SOON.asString(),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }
    }
}
