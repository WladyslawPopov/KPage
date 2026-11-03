package io.github.wladyslawpopov.kpage.demo.android.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import io.github.wladyslawpopov.kpage.demo.android.ui.rememberRenderedState
import io.github.wladyslawpopov.kpage.demo.presentation.screens.cache.CacheComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.cache.CacheState
import io.github.wladyslawpopov.kpage.demo.presentation.screens.cache.CacheUi
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.ChatComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.ChatState
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.ChatUi
import io.github.wladyslawpopov.kpage.demo.presentation.screens.lab.LabComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.lab.LabState
import io.github.wladyslawpopov.kpage.demo.presentation.screens.lab.LabUi
import io.github.wladyslawpopov.kpage.demo.presentation.screens.settings.SettingsComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.settings.SettingsState
import io.github.wladyslawpopov.kpage.demo.presentation.screens.settings.SettingsUi
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.SpaceComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.SpaceState
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.SpaceUi

@Composable
fun SpaceScreen(component: SpaceComponent) {
    val state by rememberRenderedState<SpaceState>(component) { push ->
        component.bindSpace(object : SpaceUi {
            override fun renderSpace(state: SpaceState) = push(state)
        })
    }
    state?.let { PlaceholderScreen(it.topBar, it.text) }
}

@Composable
fun ChatScreen(component: ChatComponent) {
    val state by rememberRenderedState<ChatState>(component) { push ->
        component.bindChat(object : ChatUi {
            override fun renderChat(state: ChatState) = push(state)
        })
    }
    state?.let { PlaceholderScreen(it.topBar, it.text) }
}

@Composable
fun LabScreen(component: LabComponent) {
    val state by rememberRenderedState<LabState>(component) { push ->
        component.bindLab(object : LabUi {
            override fun renderLab(state: LabState) = push(state)
        })
    }
    state?.let { PlaceholderScreen(it.topBar, it.text) }
}

@Composable
fun CacheScreen(component: CacheComponent) {
    val state by rememberRenderedState<CacheState>(component) { push ->
        component.bindCache(object : CacheUi {
            override fun renderCache(state: CacheState) = push(state)
        })
    }
    state?.let { PlaceholderScreen(it.topBar, it.text) }
}

@Composable
fun SettingsScreen(component: SettingsComponent) {
    val state by rememberRenderedState<SettingsState>(component) { push ->
        component.bindSettings(object : SettingsUi {
            override fun renderSettings(state: SettingsState) = push(state)
        })
    }
    state?.let { PlaceholderScreen(it.topBar, it.text) }
}
