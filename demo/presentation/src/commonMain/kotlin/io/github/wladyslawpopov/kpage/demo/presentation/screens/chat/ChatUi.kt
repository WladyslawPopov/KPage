package io.github.wladyslawpopov.kpage.demo.presentation.screens.chat

/** UI of the Chat screen; every platform implements it and draws the state it gets. */
interface ChatUi {
    fun renderChat(state: ChatState)
}
