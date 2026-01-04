package io.github.wladyslawpopov.kpager

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "KPager",
    ) {
        App()
    }
}