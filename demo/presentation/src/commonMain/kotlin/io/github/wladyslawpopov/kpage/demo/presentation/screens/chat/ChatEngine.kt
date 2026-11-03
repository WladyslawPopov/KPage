package io.github.wladyslawpopov.kpage.demo.presentation.screens.chat

import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatRepository
import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentTasks
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageIntent

/** Business scenarios of the Chat tab. */
internal class ChatEngine(
    private val tasks: ComponentTasks,
    private val repository: ChatRepository,
    private val getState: () -> ChatState,
    private val updateState: ((ChatState) -> ChatState) -> Unit,
    private val onPageIntent: (PageIntent) -> Unit,
) {
    fun start() = Unit

    fun onIntent(intent: ChatIntent) = Unit
}
