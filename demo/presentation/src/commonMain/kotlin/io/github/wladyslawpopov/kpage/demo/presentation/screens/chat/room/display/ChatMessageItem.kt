package io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room.display

import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatAttachment
import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatMessage
import io.github.wladyslawpopov.kpage.demo.domain.chat.MessageSendState
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room.ChatRoomIntent
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.text.UiText
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey

/**
 * A message as the room draws it, a server one or a local one.
 *
 * @property key The identity of its row; a local message keeps it when the server confirms it.
 * @property sendState Null for a message the server has; otherwise the state of the local one.
 * @property actions The menu of the message, opened by a long press.
 */
data class ChatMessageItem(
    val key: String,
    val text: String,
    val time: UiText,
    val isMine: Boolean,
    val attachments: List<ChatAttachment>,
    val sendState: MessageSendState?,
    val actions: List<ChatAction>,
)

/**
 * An entry of a menu of the room: of a message or of the chat. The screen sends [intent] as it is;
 * when [copyText] is set, it puts the text into the clipboard first (the clipboard belongs to the
 * platform).
 */
data class ChatAction(
    val title: AppStringKey,
    val icon: AppIconKey,
    val intent: ChatRoomIntent,
    val copyText: String? = null,
)

/** The rows of the room that are not server messages; they are added with `decorate`. */
sealed class ChatDecoration {
    /** An outgoing message the server has not confirmed yet. */
    data class Local(val item: ChatMessageItem) : ChatDecoration()

    /** The day of the messages under it. */
    data class DateSeparator(val date: UiText, val key: String) : ChatDecoration()
}

/**
 * The identity of the row of a server message. A message that confirms a local one takes the key of
 * the local row, so the row does not blink when the server message replaces it.
 */
fun ChatMessage.rowKey(): String = clientId?.let(::localRowKey) ?: "msg_$id"

internal fun localRowKey(localId: String): String = "local_$localId"
