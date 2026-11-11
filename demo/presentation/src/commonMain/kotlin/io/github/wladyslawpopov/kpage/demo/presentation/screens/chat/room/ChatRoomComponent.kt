package io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room

import io.github.wladyslawpopov.kpage.demo.domain.cache.CacheRepository
import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatMessage
import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatRepository
import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentContext
import io.github.wladyslawpopov.kpage.demo.presentation.component.StateOwner
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.AppNavigator
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.ScreenComponent
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageComponent
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageIntent
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageOwner
import io.github.wladyslawpopov.kpage.demo.presentation.page.ToastType
import io.github.wladyslawpopov.kpage.demo.presentation.paginator.PaginatorUiState
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room.display.ChatMessageItem
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey

interface ChatRoomComponent : ScreenComponent, StateOwner<ChatRoomState, ChatRoomIntent>, PageOwner {
    /** What the room draws for a server message: its text, time and menu. */
    fun messageItem(message: ChatMessage): ChatMessageItem
}

/** A chat: its messages, the local ones being sent, the field with the draft and the menu of the chat. */
internal class DefaultChatRoomComponent(
    context: ComponentContext,
    private val navigator: AppNavigator,
    chatId: Long,
    repository: ChatRepository,
    cache: CacheRepository,
) : PageComponent<ChatRoomState, ChatRoomIntent>(
    context,
    ChatRoomState(list = PaginatorUiState(chatId)),
), ChatRoomComponent {

    private val outgoing = OutgoingEngine(
        tasks = this,
        chatId = chatId,
        repository = repository,
        states = state,
        onChanged = { messages.updateMessages() },
        onPageIntent = ::onPageIntent,
    )

    private val messages: MessagesEngine = MessagesEngine(
        tasks = this,
        chatId = chatId,
        repository = repository,
        cache = cache,
        outgoing = outgoing,
        getState = { currentState },
        updateState = ::updateState,
        onPageIntent = ::onPageIntent,
    )

    private val composer = ComposerEngine(
        tasks = this,
        chatId = chatId,
        repository = repository,
        getState = { currentState },
        updateState = ::updateState,
        onPageIntent = ::onPageIntent,
    )

    private val info = ChatInfoEngine(
        tasks = this,
        chatId = chatId,
        repository = repository,
        getState = { currentState },
        updateState = ::updateState,
        onPageIntent = ::onPageIntent,
        // Navigation belongs to the component: the deleted chat closes.
        onDeleted = { navigator.back() },
    )

    init {
        messages.start()
        outgoing.start()
        composer.start()
        info.refresh()
    }

    override fun onIntent(intent: ChatRoomIntent) {
        when (intent) {
            is ChatRoomIntent.DeleteMessage -> messages.deleteMessage(intent.id)
            is ChatRoomIntent.DeleteOutgoing -> outgoing.delete(intent.localId)
            ChatRoomIntent.Copied -> onPageIntent(PageIntent.ShowToast(AppStringKey.Core.COPIED, ToastType.SUCCESS))
            ChatRoomIntent.AskDeleteChat, ChatRoomIntent.ConfirmDeleteChat, ChatRoomIntent.CancelDeleteChat ->
                info.onIntent(intent)

            else -> composer.onIntent(intent)
        }
    }

    override fun messageItem(message: ChatMessage): ChatMessageItem = messages.messageItem(message)

    override fun onRefresh() {
        messages.refresh()
        info.refresh()
    }
}
