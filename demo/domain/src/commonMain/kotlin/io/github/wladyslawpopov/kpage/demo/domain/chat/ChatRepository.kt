package io.github.wladyslawpopov.kpage.demo.domain.chat

import io.github.wladyslawpopov.kpage.core.paging.data.paginator.Paginator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

/** Data of the Chat tab: the chats and their messages on the server, the outgoing messages and drafts here. */
interface ChatRepository {
    /** The chats, the one with the newest message first; the paginator lives as long as [scope]. */
    fun chats(scope: CoroutineScope): Paginator<Chat>

    /** The messages of a chat, newest first (position 0 is the newest one). */
    fun messages(chatId: Long, scope: CoroutineScope): Paginator<ChatMessage>

    /**
     * Emits the id of a chat after every change of it on the server, whoever made it (a sent message,
     * an answer, a removal, a new chat), so the lists know when to `refresh()`.
     */
    val changes: Flow<Long>

    suspend fun chat(id: Long): Chat?

    suspend fun createChat(title: String, interlocutor: String): Chat

    suspend fun deleteChats(ids: Collection<Long>)

    suspend fun deleteMessage(chatId: Long, id: Long)

    /** Sends the message to the server; sending the same [OutgoingMessage.localId] twice makes one message. */
    suspend fun send(message: OutgoingMessage)

    /** The outgoing messages of a chat, oldest attempt first. */
    fun observeOutgoing(chatId: Long): Flow<List<OutgoingMessage>>

    /** Saves a new outgoing message as being sent and clears the draft of its chat, in one step. */
    suspend fun stageOutgoing(message: OutgoingMessage)

    /** Marks a failed message as being sent again; null if it is not failed or is being sent already. */
    suspend fun retryOutgoing(chatId: Long, localId: String, attemptedTs: Long): OutgoingMessage?

    /** Ends the attempt started by [stageOutgoing] or [retryOutgoing]. */
    suspend fun finishAttempt(chatId: Long, localId: String, failed: Boolean)

    /** A message left "being sent" by a closed app has no attempt behind it: it becomes failed. */
    suspend fun recoverInterrupted(chatId: Long)

    /** Removes an outgoing message that is not being sent right now. */
    suspend fun deleteOutgoing(chatId: Long, localId: String)

    /** Removes the outgoing messages the server has confirmed. */
    suspend fun resolveOutgoing(chatId: Long, localIds: Collection<String>)

    suspend fun draft(chatId: Long): ChatDraft?

    /** Saves the draft; an empty one is removed. */
    suspend fun saveDraft(draft: ChatDraft)
}
