package io.github.wladyslawpopov.kpage.demo.data.chat

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.github.wladyslawpopov.kpage.cache.PagingDatabase
import io.github.wladyslawpopov.kpage.core.paging.StablePaginator
import io.github.wladyslawpopov.kpage.core.paging.data.paginator.Paginator
import io.github.wladyslawpopov.kpage.core.paging.data.paginator.PaginatorConfig
import io.github.wladyslawpopov.kpage.demo.data.db.Chat_outgoing
import io.github.wladyslawpopov.kpage.demo.data.db.DemoDatabase
import io.github.wladyslawpopov.kpage.demo.domain.chat.Chat
import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatAttachment
import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatDraft
import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatMessage
import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatRepository
import io.github.wladyslawpopov.kpage.demo.domain.chat.MessageSendState
import io.github.wladyslawpopov.kpage.demo.domain.chat.OutgoingMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * The chats and the messages come from [ChatServer] through paginators; the outgoing messages and
 * the drafts are kept in the local tables of the demo's database.
 */
internal class ChatRepositoryImpl(
    private val server: ChatServer,
    demoDb: DemoDatabase,
    private val db: PagingDatabase,
    private val json: Json,
) : ChatRepository {
    private val local = demoDb.chatLocalQueries
    private val localLock = Mutex()

    // The messages with a send running in this process; anything else "being sent" was interrupted.
    private val activeAttempts = mutableSetOf<String>()
    private val attachmentsSerializer = ListSerializer(ChatAttachment.serializer())

    override val changes: Flow<Long> = server.changes

    override fun chats(scope: CoroutineScope): Paginator<Chat> =
        StablePaginator(
            serializer = Chat.serializer(),
            queryKey = CHATS_KEY,
            config = PaginatorConfig(pageSize = CHATS_PAGE_SIZE),
            getPage = { page -> server.chatsPage(page, CHATS_PAGE_SIZE) },
            idExtractor = { it.id.toString() },
            db = db,
            scope = scope,
        )

    override fun messages(chatId: Long, scope: CoroutineScope): Paginator<ChatMessage> =
        StablePaginator(
            serializer = ChatMessage.serializer(),
            queryKey = "chat|$chatId",
            config = PaginatorConfig(pageSize = MESSAGES_PAGE_SIZE),
            getPage = { page -> server.messagesPage(chatId, page, MESSAGES_PAGE_SIZE) },
            idExtractor = { it.id.toString() },
            db = db,
            scope = scope,
        )

    override suspend fun chat(id: Long): Chat? = server.chat(id)

    override suspend fun createChat(title: String, interlocutor: String): Chat = server.createChat(title, interlocutor)

    override suspend fun deleteChats(ids: Collection<Long>) {
        server.deleteChats(ids)
        locked {
            local.transaction {
                ids.forEach {
                    local.deleteOutgoingByChat(it)
                    local.deleteDraft(it)
                }
            }
        }
    }

    override suspend fun deleteMessage(chatId: Long, id: Long) = server.deleteMessage(chatId, id)

    override suspend fun send(message: OutgoingMessage) {
        server.send(message.chatId, message.localId, message.text, message.attachments)
    }

    override fun observeOutgoing(chatId: Long): Flow<List<OutgoingMessage>> =
        local.selectOutgoing(chatId).asFlow().mapToList(Dispatchers.IO).map { rows -> rows.map { it.toDomain() } }

    override suspend fun stageOutgoing(message: OutgoingMessage) = locked {
        local.transaction {
            local.insertOutgoing(
                message.localId, message.chatId, message.text, encode(message.attachments),
                message.createdTs, message.attemptedTs, message.state.name,
            )
            local.deleteDraft(message.chatId)
        }
        activeAttempts.add(message.localId)
        Unit
    }

    override suspend fun retryOutgoing(chatId: Long, localId: String, attemptedTs: Long): OutgoingMessage? = locked {
        if (localId in activeAttempts) return@locked null
        val row = local.selectOutgoingById(chatId, localId).executeAsOneOrNull() ?: return@locked null
        if (row.state != MessageSendState.FAILED.name) return@locked null
        local.retryOutgoing(MessageSendState.SENDING.name, attemptedTs, chatId, localId)
        activeAttempts.add(localId)
        row.toDomain().copy(state = MessageSendState.SENDING, attemptedTs = attemptedTs)
    }

    override suspend fun finishAttempt(chatId: Long, localId: String, failed: Boolean) = locked {
        activeAttempts.remove(localId)
        if (failed) local.setOutgoingState(MessageSendState.FAILED.name, chatId, localId)
    }

    override suspend fun recoverInterrupted(chatId: Long) = locked {
        local.transaction {
            local.selectOutgoing(chatId).executeAsList()
                .filter { it.state == MessageSendState.SENDING.name && it.local_id !in activeAttempts }
                .forEach { local.setOutgoingState(MessageSendState.FAILED.name, chatId, it.local_id) }
        }
    }

    override suspend fun deleteOutgoing(chatId: Long, localId: String) = locked {
        if (localId !in activeAttempts) local.deleteOutgoing(chatId, localId)
    }

    override suspend fun resolveOutgoing(chatId: Long, localIds: Collection<String>) = locked {
        local.transaction { localIds.forEach { local.deleteOutgoing(chatId, it) } }
    }

    override suspend fun draft(chatId: Long): ChatDraft? = locked {
        local.selectDraft(chatId).executeAsOneOrNull()?.let {
            ChatDraft(it.chat_id, it.body, json.decodeFromString(attachmentsSerializer, it.attachments), it.updated_ts)
        }
    }

    override suspend fun saveDraft(draft: ChatDraft) {
        locked {
            if (draft.isEmpty) {
                local.deleteDraft(draft.chatId)
            } else {
                local.saveDraft(draft.chatId, draft.text, encode(draft.attachments), draft.updatedTs)
            }
        }
    }

    private fun encode(attachments: List<ChatAttachment>) = json.encodeToString(attachmentsSerializer, attachments)

    private fun Chat_outgoing.toDomain() = OutgoingMessage(
        localId = local_id,
        chatId = chat_id,
        text = body,
        attachments = json.decodeFromString(attachmentsSerializer, attachments),
        createdTs = created_ts,
        attemptedTs = attempted_ts,
        state = MessageSendState.valueOf(state),
    )

    // One local operation at a time, off the main thread.
    private suspend fun <T> locked(block: () -> T): T =
        withContext(Dispatchers.IO) { localLock.withLock { block() } }

    private companion object {
        const val CHATS_KEY = "chats"
        const val CHATS_PAGE_SIZE = 20
        const val MESSAGES_PAGE_SIZE = 30
    }
}
