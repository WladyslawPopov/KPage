package io.github.wladyslawpopov.kpage.demo.data.chat

import io.github.wladyslawpopov.kpage.core.paging.data.paginator.PagerPayload
import io.github.wladyslawpopov.kpage.demo.data.db.Chat_message
import io.github.wladyslawpopov.kpage.demo.data.db.DemoDatabase
import io.github.wladyslawpopov.kpage.demo.data.mock.ChatScript
import io.github.wladyslawpopov.kpage.demo.data.mock.MockServer
import io.github.wladyslawpopov.kpage.demo.data.mock.script
import io.github.wladyslawpopov.kpage.demo.domain.chat.Chat
import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatAttachment
import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatMessage
import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatScenario
import io.github.wladyslawpopov.kpage.demo.domain.error.ServerException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.time.Clock
import io.github.wladyslawpopov.kpage.demo.data.db.Chat as ChatRow

/**
 * The chat server inside the app: the chats and the messages live in the demo's database, every call
 * waits and fails like the network through [MockServer], and the interlocutor of a chat answers by
 * its [ChatScript]. The interlocutors speak only while the app is open.
 */
internal class ChatServer(
    db: DemoDatabase,
    private val mock: MockServer,
    private val json: Json,
    private val random: Random = Random.Default,
) {
    private val chats = db.chatQueries
    private val messages = db.chatMessageQueries
    private val attachmentsSerializer = ListSerializer(ChatAttachment.serializer())

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val changed = MutableSharedFlow<Long>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** The id of a chat after every change of it. */
    val changes: Flow<Long> = changed

    suspend fun chatsPage(page: Int, pageSize: Int): PagerPayload<Chat> = mock.call {
        io {
            chats.transactionWithResult {
                val total = chats.count().executeAsOne().toInt()
                val rows = chats.selectPage(pageSize.toLong(), page.toLong() * pageSize).executeAsList()
                PagerPayload(totalCount = total, isMore = (page + 1) * pageSize < total, objects = rows.map { it.toChat() })
            }
        }
    }

    /** A page of the messages of a chat, newest first. */
    suspend fun messagesPage(chatId: Long, page: Int, pageSize: Int): PagerPayload<ChatMessage> = mock.call {
        io {
            messages.transactionWithResult {
                val total = messages.count(chatId).executeAsOne().toInt()
                val rows = messages.selectPage(chatId, pageSize.toLong(), page.toLong() * pageSize).executeAsList()
                PagerPayload(totalCount = total, isMore = (page + 1) * pageSize < total, objects = rows.map { it.toMessage() })
            }
        }
    }

    suspend fun chat(id: Long): Chat? = mock.call { io { chats.selectById(id).executeAsOneOrNull()?.toChat() } }

    /** Creates a chat with a scenario chosen at random and the history of that scenario. */
    suspend fun createChat(title: String, interlocutor: String): Chat = mock.call {
        val now = now()
        val scenario = ChatScenario.entries.random(random)
        val seed = random.nextLong()
        val chat = io {
            chats.transactionWithResult {
                val history = scenario.script().history(Random(seed))
                chats.insert(title, interlocutor, scenario.name, seed, history.lastOrNull()?.text.orEmpty(), now)
                val id = chats.lastInsertId().executeAsOne()
                history.forEach {
                    messages.insert(id, if (it.isMine) 1 else 0, it.text, NO_ATTACHMENTS, now - it.age.inWholeMilliseconds, null)
                }
                chats.selectById(id).executeAsOne().toChat()
            }
        }
        changed.tryEmit(chat.id)
        chat
    }

    suspend fun deleteChats(ids: Collection<Long>) = mock.call {
        io {
            chats.transaction {
                ids.forEach {
                    messages.deleteByChat(it)
                    chats.delete(it)
                }
            }
        }
        ids.forEach(changed::tryEmit)
    }

    suspend fun deleteMessage(chatId: Long, id: Long) {
        mock.call {
            io {
                messages.transaction {
                    messages.delete(id)
                    // The chat shows its newest message that is left.
                    val chat = chats.selectById(chatId).executeAsOneOrNull() ?: return@transaction
                    val newest = messages.selectNewest(chatId).executeAsOneOrNull()
                    chats.touch(newest?.preview().orEmpty(), newest?.created_ts ?: chat.updated_ts, chatId)
                }
            }
        }
        changed.tryEmit(chatId)
    }

    /**
     * Stores the message of the user and starts the answer of the interlocutor. A message with a
     * [clientId] the chat already has is returned as it is, so a repeated send makes no second one.
     */
    suspend fun send(chatId: Long, clientId: String, text: String, attachments: List<ChatAttachment>): ChatMessage =
        mock.call {
            val chat = io { chats.selectById(chatId).executeAsOneOrNull() }
                ?: throw ServerException(status = 404, message = "No such chat")
            val script = ChatScenario.valueOf(chat.scenario).script()
            if (random.nextFloat() < script.failureRate) throw ServerException(status = 503, message = "Mock server failure")
            val now = now()
            val (message, isNew) = io {
                messages.transactionWithResult {
                    val existing = messages.selectByClientId(chatId, clientId).executeAsOneOrNull()
                    if (existing != null) {
                        existing.toMessage() to false
                    } else {
                        messages.insert(chatId, 1, text, json.encodeToString(attachmentsSerializer, attachments), now, clientId)
                        val stored = messages.selectById(messages.lastInsertId().executeAsOne()).executeAsOne()
                        chats.touch(stored.preview(), now, chatId)
                        stored.toMessage() to true
                    }
                }
            }
            if (isNew) {
                changed.tryEmit(chatId)
                scope.launch { answer(chatId, script, text, Random(chat.seed + message.id)) }
            }
            message
        }

    private suspend fun answer(chatId: Long, script: ChatScript, text: String, random: Random) {
        for (reply in script.replies(text, random)) {
            delay(reply.delay)
            val now = now()
            val stored = io {
                messages.transactionWithResult {
                    // The chat may have been deleted while the interlocutor was typing.
                    if (chats.selectById(chatId).executeAsOneOrNull() == null) return@transactionWithResult false
                    messages.insert(chatId, 0, reply.text, NO_ATTACHMENTS, now, null)
                    chats.touch(reply.text, now, chatId)
                    true
                }
            }
            if (!stored) return
            changed.tryEmit(chatId)
        }
    }

    private fun ChatRow.toChat() = Chat(
        id = id,
        title = title,
        interlocutor = interlocutor,
        scenario = ChatScenario.valueOf(scenario),
        lastMessage = last_message,
        updatedTs = updated_ts,
    )

    private fun Chat_message.toMessage() = ChatMessage(
        id = id,
        chatId = chat_id,
        isMine = is_mine != 0L,
        text = body,
        attachments = json.decodeFromString(attachmentsSerializer, attachments),
        createdTs = created_ts,
        clientId = client_id,
    )

    // A message of photos alone has no text to show in the list of chats.
    private fun Chat_message.preview() = body.ifBlank { PHOTO_PREVIEW }

    private fun now() = Clock.System.now().toEpochMilliseconds()

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }

    private companion object {
        const val NO_ATTACHMENTS = "[]"
        const val PHOTO_PREVIEW = "📷"
    }
}
