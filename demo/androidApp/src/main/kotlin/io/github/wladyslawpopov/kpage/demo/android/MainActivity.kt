package io.github.wladyslawpopov.kpage.demo.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.wladyslawpopov.kpage.demo.android.theme.DemoTheme
import io.github.wladyslawpopov.kpage.demo.android.ui.AppRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { DemoTheme { AppRoot() } }
    }
}
