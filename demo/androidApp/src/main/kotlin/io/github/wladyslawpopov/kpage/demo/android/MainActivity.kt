package io.github.wladyslawpopov.kpage.demo.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import coil3.compose.setSingletonImageLoaderFactory
import io.github.wladyslawpopov.kpage.demo.android.theme.DemoTheme
import io.github.wladyslawpopov.kpage.demo.android.ui.AppRoot
import io.github.wladyslawpopov.kpage.demo.android.ui.widgets.createImageLoader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            setSingletonImageLoaderFactory(::createImageLoader)
            DemoTheme { AppRoot() }
        }
    }
}
