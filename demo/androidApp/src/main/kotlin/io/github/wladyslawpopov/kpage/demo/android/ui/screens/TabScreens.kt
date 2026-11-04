package io.github.wladyslawpopov.kpage.demo.android.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.wladyslawpopov.kpage.demo.presentation.screens.cache.CacheComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.ChatComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.lab.LabComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.settings.SettingsComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.SpaceComponent

@Composable
fun SpaceScreen(component: SpaceComponent) {
    val state by component.state.collectAsState()
    PlaceholderScreen(state.topBar, state.text)
}

@Composable
fun ChatScreen(component: ChatComponent) {
    val state by component.state.collectAsState()
    PlaceholderScreen(state.topBar, state.text)
}

@Composable
fun LabScreen(component: LabComponent) {
    val state by component.state.collectAsState()
    PlaceholderScreen(state.topBar, state.text)
}

@Composable
fun CacheScreen(component: CacheComponent) {
    val state by component.state.collectAsState()
    PlaceholderScreen(state.topBar, state.text)
}

@Composable
fun SettingsScreen(component: SettingsComponent) {
    val state by component.state.collectAsState()
    PlaceholderScreen(state.topBar, state.text)
}
