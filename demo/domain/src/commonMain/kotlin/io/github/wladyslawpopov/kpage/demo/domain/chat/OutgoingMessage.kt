package io.github.wladyslawpopov.kpage.demo.domain.chat

enum class MessageSendState { SENDING, FAILED }

/**
 * A message the user sent that the server has not confirmed yet. It is kept in the local database,
 * so it survives a restart; [localId] goes to the server as the client id of the message.
 */
data class OutgoingMessage(
    val localId: String,
    val chatId: Long,
    val text: String,
    val attachments: List<ChatAttachment>,
    val createdTs: Long,
    val attemptedTs: Long = createdTs,
    val state: MessageSendState = MessageSendState.SENDING,
)

/** What the user has typed into a chat and not sent. */
data class ChatDraft(
    val chatId: Long,
    val text: String = "",
    val attachments: List<ChatAttachment> = emptyList(),
    val updatedTs: Long = 0,
) {
    val isEmpty: Boolean get() = text.isEmpty() && attachments.isEmpty()
}
