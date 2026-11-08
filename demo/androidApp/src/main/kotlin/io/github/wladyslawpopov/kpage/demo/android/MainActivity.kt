package io.github.wladyslawpopov.kpage.demo.android

import android.os.Build
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
        // The bottom bar of the app paints the area of the navigation buttons itself.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
        setContent {
            setSingletonImageLoaderFactory(::createImageLoader)
            DemoTheme { AppRoot() }
        }
    }
}
