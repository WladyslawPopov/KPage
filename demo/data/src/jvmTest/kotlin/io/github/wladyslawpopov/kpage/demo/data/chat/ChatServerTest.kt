package io.github.wladyslawpopov.kpage.demo.data.chat

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.wladyslawpopov.kpage.demo.data.db.DemoDatabase
import io.github.wladyslawpopov.kpage.demo.data.mock.MockServer
import io.github.wladyslawpopov.kpage.demo.data.mock.MockServerConfig
import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatScenario
import io.github.wladyslawpopov.kpage.demo.domain.error.ServerException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import java.util.Properties
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ChatServerTest {

    private val db = DemoDatabase(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties(), DemoDatabase.Schema))
    private val server = ChatServer(
        db = db,
        mock = MockServer(MockServerConfig(latency = Duration.ZERO), Random(1)),
        json = Json { ignoreUnknownKeys = true; explicitNulls = false },
        random = Random(1),
    )

    /** A chat with a chosen scenario and no history, so the tests know exactly what is in it. */
    private fun chat(scenario: ChatScenario = ChatScenario.CHATTY): Long {
        db.chatQueries.insert("Title", "Bot", scenario.name, 7L, "", 0L)
        return db.chatQueries.lastInsertId().executeAsOne()
    }

    private fun addMessage(chatId: Long, text: String, isMine: Boolean = true, ts: Long = 0L) {
        db.chatMessageQueries.insert(chatId, if (isMine) 1 else 0, text, "[]", ts, null)
    }

    private fun <T> blocking(block: suspend CoroutineScope.() -> T): T = runBlocking { withTimeout(10.seconds) { block() } }

    private suspend fun mine(chatId: Long) =
        server.messagesPage(chatId, page = 0, pageSize = 1000).objects.filter { it.isMine }

    @Test
    fun sending_again_with_the_same_client_id_makes_one_message() = blocking {
        val chatId = chat()

        val first = server.send(chatId, clientId = "c1", text = "hello", attachments = emptyList())
        val second = server.send(chatId, clientId = "c1", text = "hello", attachments = emptyList())

        assertEquals(first.id, second.id)
        // The interlocutor answers later; only the user's own messages are counted.
        assertEquals(listOf("hello"), mine(chatId).map { it.text })
    }

    @Test
    fun different_client_ids_make_different_messages() = blocking {
        val chatId = chat()

        server.send(chatId, "c1", "one", emptyList())
        server.send(chatId, "c2", "two", emptyList())

        assertEquals(setOf("one", "two"), mine(chatId).map { it.text }.toSet())
    }

    @Test
    fun sending_to_a_missing_chat_is_a_404() = blocking {
        val error = assertFailsWith<ServerException> { server.send(chatId = 999, clientId = "c1", text = "hi", attachments = emptyList()) }

        assertEquals(404, error.status)
    }

    @Test
    fun a_sent_message_moves_into_the_preview_of_the_chat() = blocking {
        val chatId = chat()

        server.send(chatId, "c1", "hello", emptyList())

        assertEquals("hello", server.chat(chatId)?.lastMessage)
    }

    @Test
    fun a_flaky_chat_sometimes_refuses_and_a_refused_message_is_not_stored() = blocking {
        val chatId = chat(ChatScenario.FLAKY)

        val results = (1..30).map { runCatching { server.send(chatId, "c$it", "m$it", emptyList()) } }

        val refused = results.mapNotNull { it.exceptionOrNull() }
        assertTrue(refused.isNotEmpty(), "failureRate 0.4 over 30 sends must refuse some")
        assertTrue(refused.all { it is ServerException && it.status == 503 })
        assertTrue(results.any { it.isSuccess }, "and accept some")
        assertEquals(results.count { it.isSuccess }, mine(chatId).size)
    }

    @Test
    fun pages_go_from_the_newest_message_to_the_oldest() = blocking {
        val chatId = chat()
        (1..5).forEach { addMessage(chatId, "m$it", ts = it.toLong()) }

        val first = server.messagesPage(chatId, page = 0, pageSize = 2)
        val last = server.messagesPage(chatId, page = 2, pageSize = 2)

        assertEquals(listOf("m5", "m4"), first.objects.map { it.text })
        assertEquals(5, first.totalCount)
        assertTrue(first.isMore)
        assertEquals(listOf("m1"), last.objects.map { it.text })
        assertTrue(!last.isMore)
    }

    @Test
    fun deleting_the_newest_message_puts_the_previous_one_into_the_preview() = blocking {
        val chatId = chat()
        addMessage(chatId, "old", ts = 1)
        addMessage(chatId, "new", ts = 2)
        val newest = server.messagesPage(chatId, 0, 1).objects.single()

        server.deleteMessage(chatId, newest.id)

        assertEquals("old", server.chat(chatId)?.lastMessage)
    }

    @Test
    fun deleting_a_chat_removes_its_messages_and_announces_the_change() = blocking {
        val chatId = chat()
        addMessage(chatId, "m1")
        val announced = async(start = CoroutineStart.UNDISPATCHED) { server.changes.first() }

        server.deleteChats(listOf(chatId))

        assertEquals(chatId, announced.await())
        assertEquals(null, server.chat(chatId))
        assertEquals(0, server.messagesPage(chatId, 0, 10).totalCount)
    }

    @Test
    fun a_new_chat_gets_the_history_of_its_scenario() = blocking {
        val chat = server.createChat("Title", "Bot")

        val page = server.messagesPage(chat.id, page = 0, pageSize = 1000)

        assertTrue((page.totalCount ?: 0) > 0)
        assertEquals(chat.lastMessage, server.messagesPage(chat.id, 0, 1).objects.single().text)
    }
}
