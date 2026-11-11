package io.github.wladyslawpopov.kpage.demo.presentation.testing

import io.github.wladyslawpopov.kpage.core.paging.data.paginator.Paginator
import io.github.wladyslawpopov.kpage.demo.domain.chat.Chat
import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatDraft
import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatMessage
import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatRepository
import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatScenario
import io.github.wladyslawpopov.kpage.demo.domain.chat.OutgoingMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A [ChatRepository] of one chat that remembers what the code under test asked of it. Local messages
 * behave like the real ones: resolving removes them from [outgoing].
 */
internal class FakeChatRepository : ChatRepository {
    /** What happened, in order: "stage", "send", "finish:ok", "finish:failed", "retry". */
    val events = mutableListOf<String>()
    val staged = mutableListOf<OutgoingMessage>()
    val sent = mutableListOf<OutgoingMessage>()
    val drafts = mutableListOf<ChatDraft>()
    val resolved = mutableListOf<Collection<String>>()
    val deletedMessages = mutableListOf<Long>()
    val deletedOutgoing = mutableListOf<String>()
    var savedDraft: ChatDraft? = null
    var sendError: Throwable? = null
    var sendGate: CompletableDeferred<Unit>? = null
    var deleteGate: CompletableDeferred<Unit>? = null
    var retryResult: OutgoingMessage? = null
    val deletedChats = mutableListOf<Collection<Long>>()
    val createdChats = mutableListOf<Chat>()
    var createGate: CompletableDeferred<Unit>? = null
    val chatsPaginator = FakePaginator<Chat>()

    val outgoing = MutableStateFlow<List<OutgoingMessage>>(emptyList())
    val messagePaginator = FakePaginator<ChatMessage>()
    override val changes = MutableSharedFlow<Long>()

    override fun messages(chatId: Long, scope: CoroutineScope): Paginator<ChatMessage> = messagePaginator

    override fun observeOutgoing(chatId: Long): Flow<List<OutgoingMessage>> = outgoing

    override suspend fun resolveOutgoing(chatId: Long, localIds: Collection<String>) {
        resolved += localIds
        outgoing.value = outgoing.value.filter { it.localId !in localIds }
    }

    override suspend fun deleteMessage(chatId: Long, id: Long) {
        deletedMessages += id
        deleteGate?.await()
    }

    override suspend fun deleteOutgoing(chatId: Long, localId: String) {
        deletedOutgoing += localId
    }

    override suspend fun draft(chatId: Long) = savedDraft

    override suspend fun saveDraft(draft: ChatDraft) {
        drafts += draft
    }

    override suspend fun stageOutgoing(message: OutgoingMessage) {
        events += "stage"
        staged += message
    }

    override suspend fun send(message: OutgoingMessage) {
        events += "send"
        sent += message
        sendGate?.await()
        sendError?.let { throw it }
    }

    override suspend fun finishAttempt(chatId: Long, localId: String, failed: Boolean) {
        events += if (failed) "finish:failed" else "finish:ok"
    }

    override suspend fun retryOutgoing(chatId: Long, localId: String, attemptedTs: Long): OutgoingMessage? {
        events += "retry"
        return retryResult
    }

    override suspend fun recoverInterrupted(chatId: Long) = Unit

    override fun chats(scope: CoroutineScope): Paginator<Chat> = chatsPaginator

    override suspend fun createChat(title: String, interlocutor: String): Chat {
        createGate?.await()
        val chat = Chat(
            id = 100L + createdChats.size,
            title = title,
            interlocutor = interlocutor,
            scenario = ChatScenario.CHATTY,
            lastMessage = "",
            updatedTs = 0,
        )
        createdChats += chat
        return chat
    }

    override suspend fun deleteChats(ids: Collection<Long>) {
        deletedChats += ids
    }

    override suspend fun chat(id: Long): Chat = error("not used")
}
