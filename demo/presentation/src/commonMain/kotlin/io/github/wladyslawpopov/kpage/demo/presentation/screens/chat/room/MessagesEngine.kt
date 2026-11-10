package io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room

import io.github.wladyslawpopov.kpage.core.paging.data.state.DecorationScope
import io.github.wladyslawpopov.kpage.core.paging.data.state.LoadState
import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatMessage
import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatRepository
import io.github.wladyslawpopov.kpage.demo.domain.chat.MessageSendState
import io.github.wladyslawpopov.kpage.demo.domain.chat.OutgoingMessage
import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentTasks
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageIntent
import io.github.wladyslawpopov.kpage.demo.presentation.paginator.PaginatorEngine
import io.github.wladyslawpopov.kpage.demo.presentation.paginator.PaginatorIntent
import io.github.wladyslawpopov.kpage.demo.presentation.paginator.PaginatorUiState
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room.display.ChatDecoration
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room.display.ChatAction
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room.display.ChatMessageItem
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room.display.localRowKey
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room.display.rowKey
import io.github.wladyslawpopov.kpage.demo.presentation.time.localDay
import io.github.wladyslawpopov.kpage.demo.presentation.time.nowMillis
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.text.UiText
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf

/** Message pages of a chat: the paginator, the rows with local messages and date separators, deletion. */
internal class MessagesEngine(
    private val tasks: ComponentTasks,
    private val chatId: Long,
    private val repository: ChatRepository,
    private val outgoing: OutgoingEngine,
    private val getState: () -> ChatRoomState,
    private val updateState: ((ChatRoomState) -> ChatRoomState) -> Unit,
    private val onPageIntent: (PageIntent) -> Unit,
) {
    private val paginator = PaginatorEngine<Long, ChatMessage, ChatDecoration>(
        getState = { getState().list },
        updateState = ::setList,
        onError = { onPageIntent(PageIntent.OnError(it)) },
    )
    private var isDeletingMessage = false

    fun start() {
        tasks.launchMain {
            paginator.collectPaging(flowOf(chatId)) { id, scope -> repository.messages(id, scope) }
        }
        // A sent message, an answer, a removal: the server changed the chat, so its window is renewed.
        tasks.launchMain {
            repository.changes.filter { it == chatId }.collect { refresh() }
        }
    }

    fun refresh() = paginator.onIntent(PaginatorIntent.Refresh)

    /** Builds the rows again: the local messages have changed. */
    fun updateMessages() = paginator.decorate { decorateMessages() }

    fun messageItem(message: ChatMessage): ChatMessageItem = ChatMessageItem(
        key = message.rowKey(),
        text = message.text,
        time = UiText.Time(message.createdTs),
        isMine = message.isMine,
        attachments = message.attachments,
        sendState = null,
        actions = listOfNotNull(
            copyAction(message.text),
            ChatAction(AppStringKey.Chat.DELETE, AppIconKey.TRASH, ChatRoomIntent.DeleteMessage(message.id)),
        ),
    )

    fun deleteMessage(id: Long) {
        if (isDeletingMessage) return
        onPageIntent(PageIntent.ClearError)
        isDeletingMessage = true
        tasks.launchMain {
            try {
                // The list renews itself on the change the repository reports.
                repository.deleteMessage(chatId, id)
            } finally {
                isDeletingMessage = false
            }
        }
    }

    // Reads the snapshot it is applied to, so the local rows always match the shown server messages.
    private fun DecorationScope<ChatMessage, ChatDecoration>.decorateMessages() {
        val today = localDay(nowMillis())
        // Unconfirmed local messages stay at the bottom, the newest one is the lowest.
        outgoing.unconfirmed(items).forEach { insert(0, ChatDecoration.Local(localMessageItem(it))) }
        between { previous, next ->
            if (localDay(previous.createdTs) != localDay(next.createdTs)) dateSeparator(previous, today) else null
        }
        // The oldest message of every loaded run closes its date group.
        items.loadedIndices.forEach { index ->
            if (!items.isLoaded(index + 1)) {
                items[index]?.let { insert(index + 1, dateSeparator(it, today)) }
            }
        }
    }

    private fun dateSeparator(message: ChatMessage, today: Long): ChatDecoration.DateSeparator {
        val day = localDay(message.createdTs)
        return ChatDecoration.DateSeparator(
            date = if (day == today) UiText.Key(AppStringKey.Chat.TODAY) else UiText.Date(message.createdTs),
            key = "sep_${message.rowKey()}_$day",
        )
    }

    private fun localMessageItem(local: OutgoingMessage): ChatMessageItem {
        val actions = mutableListOf<ChatAction>()
        copyAction(local.text)?.let(actions::add)
        if (local.state == MessageSendState.FAILED) {
            actions += ChatAction(AppStringKey.Chat.RETRY, AppIconKey.SEND, ChatRoomIntent.Retry(local.localId))
            actions += ChatAction(
                AppStringKey.Chat.DELETE, AppIconKey.TRASH, ChatRoomIntent.DeleteOutgoing(local.localId),
            )
        }
        return ChatMessageItem(
            key = localRowKey(local.localId),
            text = local.text,
            time = UiText.Time(local.attemptedTs),
            isMine = true,
            attachments = local.attachments,
            sendState = local.state,
            actions = actions,
        )
    }

    private fun copyAction(text: String): ChatAction? =
        if (text.isBlank()) {
            null
        } else {
            ChatAction(AppStringKey.Chat.COPY, AppIconKey.COPY, ChatRoomIntent.Copied, copyText = text)
        }

    // The page shows a loader only before the first data: local messages alone are already content.
    private fun setList(list: PaginatorUiState<Long, ChatMessage, ChatDecoration>) {
        updateState { state ->
            state.copy(
                list = list,
                page = state.page.copy(isLoading = list.loadState is LoadState.INITIAL && list.rows.rowCount == 0),
            )
        }
    }
}
