package io.github.wladyslawpopov.kpage.demo.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "KPage Demo") { }
}
