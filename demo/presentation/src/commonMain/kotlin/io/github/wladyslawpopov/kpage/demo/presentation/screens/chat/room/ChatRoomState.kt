package io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room

import io.github.wladyslawpopov.kpage.core.paging.data.state.PagingRow
import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatAttachment
import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatMessage
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageScreenState
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageState
import io.github.wladyslawpopov.kpage.demo.presentation.paginator.PaginatorUiState
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.listing.mapper.ChatAvatar
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room.display.ChatAction
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room.display.ChatDecoration
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room.display.rowKey
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey

/**
 * @property list The messages, newest first: row 0 is the bottom of the reversed list.
 * @property avatar The picture of the interlocutor next to the title, as in the list of chats.
 * @property scenarioNote How the fake interlocutor of this chat behaves; shown as a note of the mock.
 * @property actions The menu of the chat in the bar.
 * @property isDeleteAsked The confirmation of the removal of the chat is shown.
 * @property text The message being typed; saved as the draft of the chat together with [attachments].
 */
data class ChatRoomState(
    // The list is reversed and renews itself on the changes of the chat: there is nothing to pull.
    override val page: PageState = PageState(isRefreshEnabled = false),
    val title: String = "",
    val interlocutor: String = "",
    val avatar: ChatAvatar? = null,
    val scenarioNote: AppStringKey? = null,
    val actions: List<ChatAction> = emptyList(),
    val isDeleteAsked: Boolean = false,
    val list: PaginatorUiState<Long, ChatMessage, ChatDecoration>,
    val text: String = "",
    val attachments: List<ChatAttachment> = emptyList(),
) : PageScreenState<ChatRoomState> {
    val canSend: Boolean
        get() = text.isNotBlank() || attachments.isNotEmpty()

    val canAttach: Boolean
        get() = attachments.size < MAX_ATTACHMENTS

    /**
     * The key of the bottom row: the newest server message or an unconfirmed local one. The screen
     * scrolls to the bottom when it changes.
     */
    val newestKey: String?
        get() = if (list.rows.rowCount == 0) {
            null
        } else {
            when (val row = list.rows[0]) {
                is PagingRow.Item -> row.item?.rowKey()
                is PagingRow.Decoration -> (row.value as? ChatDecoration.Local)?.item?.key
            }
        }

    override fun withPage(page: PageState) = copy(page = page)

    companion object {
        const val MAX_ATTACHMENTS = 8
    }
}
