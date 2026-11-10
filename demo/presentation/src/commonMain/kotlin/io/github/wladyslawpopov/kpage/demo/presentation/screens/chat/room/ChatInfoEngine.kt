package io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room

import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatRepository
import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatScenario
import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentTasks
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageEmptyState
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageIntent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.listing.mapper.avatar
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room.display.ChatAction
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey

/** The chat itself: its title, interlocutor and mock note in the bar, and its menu with the removal. */
internal class ChatInfoEngine(
    private val tasks: ComponentTasks,
    private val chatId: Long,
    private val repository: ChatRepository,
    private val getState: () -> ChatRoomState,
    private val updateState: ((ChatRoomState) -> ChatRoomState) -> Unit,
    private val onPageIntent: (PageIntent) -> Unit,
    private val onDeleted: () -> Unit,
) {
    private var isDeleting = false

    fun refresh() {
        tasks.launchMain {
            val chat = repository.chat(chatId)
            updateState { state ->
                if (chat == null) {
                    // The chat was deleted, for example from the list while this room was in the stack.
                    state.copy(
                        actions = emptyList(),
                        page = state.page.copy(
                            empty = PageEmptyState(
                                title = AppStringKey.Chat.NOT_FOUND,
                                icon = AppIconKey.CHAT,
                                actionText = null,
                                action = null,
                            ),
                        ),
                    )
                } else {
                    state.copy(
                        title = chat.title,
                        interlocutor = chat.interlocutor,
                        avatar = chat.avatar(),
                        scenarioNote = chat.scenario.note(),
                        actions = listOf(ChatAction(AppStringKey.Chat.DELETE_CHAT, AppIconKey.TRASH, ChatRoomIntent.AskDeleteChat)),
                    )
                }
            }
        }
    }

    fun onIntent(intent: ChatRoomIntent) {
        when (intent) {
            ChatRoomIntent.AskDeleteChat -> updateState { it.copy(isDeleteAsked = true) }
            ChatRoomIntent.CancelDeleteChat -> updateState { it.copy(isDeleteAsked = false) }
            ChatRoomIntent.ConfirmDeleteChat -> deleteChat()
            else -> Unit
        }
    }

    private fun deleteChat() {
        if (!getState().isDeleteAsked || isDeleting) return
        updateState { it.copy(isDeleteAsked = false) }
        onPageIntent(PageIntent.ClearError)
        isDeleting = true
        tasks.launchMain {
            try {
                repository.deleteChats(listOf(chatId))
                onDeleted()
            } finally {
                isDeleting = false
            }
        }
    }

    private fun ChatScenario.note(): AppStringKey = when (this) {
        ChatScenario.CHATTY -> AppStringKey.Chat.SCENARIO_CHATTY
        ChatScenario.HISTORIAN -> AppStringKey.Chat.SCENARIO_HISTORIAN
        ChatScenario.FLAKY -> AppStringKey.Chat.SCENARIO_FLAKY
    }
}
