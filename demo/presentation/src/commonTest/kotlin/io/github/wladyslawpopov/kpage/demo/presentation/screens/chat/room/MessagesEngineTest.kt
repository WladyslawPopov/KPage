package io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room

import io.github.wladyslawpopov.kpage.core.paging.data.state.LoadState
import io.github.wladyslawpopov.kpage.core.paging.data.state.PagingRow
import io.github.wladyslawpopov.kpage.core.paging.data.state.PagingState
import io.github.wladyslawpopov.kpage.core.paging.data.state.pagedListOf
import io.github.wladyslawpopov.kpage.demo.domain.cache.CacheRepository
import io.github.wladyslawpopov.kpage.demo.domain.cache.CacheStats
import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatMessage
import io.github.wladyslawpopov.kpage.demo.domain.chat.MessageSendState
import io.github.wladyslawpopov.kpage.demo.domain.chat.OutgoingMessage
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageIntent
import io.github.wladyslawpopov.kpage.demo.presentation.paginator.PaginatorUiState
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room.display.ChatDecoration
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room.display.localRowKey
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room.display.rowKey
import io.github.wladyslawpopov.kpage.demo.presentation.testing.FakeChatRepository
import io.github.wladyslawpopov.kpage.demo.presentation.testing.TestTasks
import io.github.wladyslawpopov.kpage.demo.presentation.time.localDay
import io.github.wladyslawpopov.kpage.demo.presentation.time.nowMillis
import io.github.wladyslawpopov.kpage.demo.presentation.time.utcOffsetMillis
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.text.UiText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The rows of the room are described as text: a message by its text, a gap as "slot", a local message as
 * "local:text", a date row as "sep:today" or "sep:-1" (days before today).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MessagesEngineTest {

    private class FakeCache : CacheRepository {
        override val cleared = MutableSharedFlow<Unit>()
        override suspend fun stats() = CacheStats(0, 0)
        override suspend fun clear() = Unit
    }

    private class Harness(scope: TestScope) {
        val chats = FakeChatRepository()
        val intents = mutableListOf<PageIntent>()
        val state = MutableStateFlow(ChatRoomState(list = PaginatorUiState(CHAT_ID)))
        private val tasks = TestTasks(
            scope.backgroundScope,
            StandardTestDispatcher(scope.testScheduler),
            StandardTestDispatcher(scope.testScheduler),
        )
        private val outgoing = OutgoingEngine(
            tasks = tasks,
            chatId = CHAT_ID,
            repository = chats,
            states = state,
            onChanged = { messages.updateMessages() },
            onPageIntent = { intents += it },
        )
        val messages: MessagesEngine = MessagesEngine(
            tasks = tasks,
            chatId = CHAT_ID,
            repository = chats,
            cache = FakeCache(),
            outgoing = outgoing,
            getState = { state.value },
            updateState = { state.value = it(state.value) },
            onPageIntent = { intents += it },
        )

        /** Both engines start together, as in the real screen. */
        fun start() {
            messages.start()
            outgoing.start()
        }

        /** The server has [size] messages, of which [loaded] are in the window; position 0 is the newest. */
        fun show(size: Int, loaded: Map<Int, ChatMessage>) {
            chats.messagePaginator.state.value =
                PagingState(pagedListOf(size, loaded), totalCount = size, loadState = LoadState.END, isRestored = true)
        }

        fun rows(): List<String> {
            val rows = state.value.list.rows
            return (0 until rows.rowCount).map { index ->
                when (val row = rows[index]) {
                    is PagingRow.Item -> row.item?.text ?: "slot"
                    is PagingRow.Decoration -> when (val decoration = row.value) {
                        is ChatDecoration.Local -> "local:${decoration.item.text}"
                        is ChatDecoration.DateSeparator -> "sep:" + describe(decoration.date)
                    }
                }
            }
        }

        fun localItem(text: String) = state.value.list.rows.let { rows ->
            (0 until rows.rowCount).map { rows[it] }
                .filterIsInstance<PagingRow.Decoration<ChatDecoration>>()
                .map { it.value }
                .filterIsInstance<ChatDecoration.Local>()
                .single { it.item.text == text }.item
        }

        private fun describe(date: UiText): String = when (date) {
            UiText.Key(AppStringKey.Chat.TODAY) -> "today"
            is UiText.Date -> (localDay(date.epochMillis) - localDay(nowMillis())).toString()
            else -> error("unexpected $date")
        }
    }

    private fun TestScope.started() = Harness(this).also {
        it.start()
        runCurrent()
    }

    // A noon of the local day [daysAgo] days back: far from midnight, so the day is the same in every zone.
    private fun noon(daysAgo: Int): Long {
        val now = nowMillis()
        val day = localDay(now) - daysAgo
        val moment = day * DAY + 12 * HOUR - utcOffsetMillis(now)
        check(localDay(moment) == day) { "the time zone moved the noon to another day" }
        return moment
    }

    private fun message(id: Long, text: String, daysAgo: Int, clientId: String? = null) =
        ChatMessage(id = id, chatId = CHAT_ID, isMine = false, text = text, createdTs = noon(daysAgo), clientId = clientId)

    private fun local(id: String, text: String, attemptedTs: Long, state: MessageSendState = MessageSendState.SENDING) =
        OutgoingMessage(id, CHAT_ID, text, emptyList(), createdTs = attemptedTs, attemptedTs = attemptedTs, state = state)

    @Test
    fun a_date_row_closes_every_day_of_the_list() = runTest {
        val harness = started()

        harness.show(5, mapOf(0 to message(5, "A", 0), 1 to message(4, "B", 0), 2 to message(3, "C", 1), 3 to message(2, "D", 1), 4 to message(1, "E", 2)))
        runCurrent()

        assertEquals(listOf("A", "B", "sep:today", "C", "D", "sep:-1", "E", "sep:-2"), harness.rows())
    }

    @Test
    fun a_gap_in_the_window_closes_the_day_before_it_and_adds_no_row_across_it() = runTest {
        val harness = started()

        harness.show(5, mapOf(0 to message(5, "A", 0), 1 to message(4, "B", 0), 3 to message(2, "D", 1), 4 to message(1, "E", 1)))
        runCurrent()

        assertEquals(listOf("A", "B", "sep:today", "slot", "D", "E", "sep:-1"), harness.rows())
    }

    @Test
    fun unconfirmed_local_messages_sit_at_the_bottom_the_newest_one_lowest() = runTest {
        val harness = started()
        harness.show(1, mapOf(0 to message(1, "A", 0)))
        runCurrent()

        harness.chats.outgoing.value = listOf(local("u1", "first", attemptedTs = 10), local("u2", "second", attemptedTs = 20))
        runCurrent()

        assertEquals(listOf("local:second", "local:first", "A", "sep:today"), harness.rows())
    }

    @Test
    fun a_server_message_with_the_local_id_replaces_the_local_one_and_resolves_it() = runTest {
        val harness = started()
        harness.chats.outgoing.value = listOf(local("u1", "hello", attemptedTs = 10))
        runCurrent()
        assertEquals(listOf("local:hello"), harness.rows())
        assertTrue(harness.chats.resolved.isEmpty())

        val confirmed = message(1, "hello", 0, clientId = "u1")
        harness.show(1, mapOf(0 to confirmed))
        runCurrent()

        assertEquals(listOf(listOf("u1")), harness.chats.resolved.map { it.toList() })
        assertEquals(listOf("hello", "sep:today"), harness.rows())
        // The row keeps its identity, so it does not blink when the server message takes the place.
        assertEquals(localRowKey("u1"), confirmed.rowKey())
    }

    @Test
    fun other_server_messages_do_not_resolve_a_local_one() = runTest {
        val harness = started()
        harness.chats.outgoing.value = listOf(local("u1", "hello", attemptedTs = 10))

        harness.show(1, mapOf(0 to message(1, "A", 0, clientId = "other")))
        runCurrent()

        assertTrue(harness.chats.resolved.isEmpty())
        assertEquals(listOf("local:hello", "A", "sep:today"), harness.rows())
    }

    @Test
    fun a_failed_local_message_offers_retry_and_delete_a_sending_one_only_copy() = runTest {
        val harness = started()
        harness.chats.outgoing.value = listOf(
            local("u1", "failed", attemptedTs = 10, state = MessageSendState.FAILED),
            local("u2", "sending", attemptedTs = 20),
        )
        runCurrent()

        assertEquals(
            listOf(ChatRoomIntent.Copied, ChatRoomIntent.Retry("u1"), ChatRoomIntent.DeleteOutgoing("u1")),
            harness.localItem("failed").actions.map { it.intent },
        )
        assertEquals(listOf<ChatRoomIntent>(ChatRoomIntent.Copied), harness.localItem("sending").actions.map { it.intent })
    }

    @Test
    fun a_server_message_offers_copy_and_delete_and_photos_alone_no_copy() = runTest {
        val harness = started()
        val withText = message(7, "hello", 0)
        val photosOnly = message(8, "", 0)

        assertEquals(
            listOf(ChatRoomIntent.Copied, ChatRoomIntent.DeleteMessage(7)),
            harness.messages.messageItem(withText).actions.map { it.intent },
        )
        assertEquals("hello", harness.messages.messageItem(withText).actions.first().copyText)
        assertEquals(
            listOf<ChatRoomIntent>(ChatRoomIntent.DeleteMessage(8)),
            harness.messages.messageItem(photosOnly).actions.map { it.intent },
        )
    }

    @Test
    fun a_message_is_deleted_once_at_a_time() = runTest {
        val harness = started()
        harness.chats.deleteGate = CompletableDeferred()

        harness.messages.deleteMessage(5)
        harness.messages.deleteMessage(5)
        runCurrent()
        assertEquals(listOf(5L), harness.chats.deletedMessages)
        assertEquals(PageIntent.ClearError, harness.intents.single())

        harness.chats.deleteGate?.complete(Unit)
        runCurrent()
        harness.messages.deleteMessage(6)
        runCurrent()
        assertEquals(listOf(5L, 6L), harness.chats.deletedMessages)
    }

    @Test
    fun a_change_of_this_chat_refreshes_the_list_and_a_change_of_another_one_does_not() = runTest {
        val harness = started()

        harness.chats.changes.emit(CHAT_ID + 1)
        runCurrent()
        assertEquals(0, harness.chats.messagePaginator.refreshCount)

        harness.chats.changes.emit(CHAT_ID)
        runCurrent()
        assertEquals(1, harness.chats.messagePaginator.refreshCount)
    }

    @Test
    fun the_loader_shows_only_before_the_first_data() = runTest {
        val harness = started()
        assertTrue(harness.state.value.page.isLoading)

        harness.show(1, mapOf(0 to message(1, "A", 0)))
        runCurrent()

        assertFalse(harness.state.value.page.isLoading)
    }

    private companion object {
        const val CHAT_ID = 1L
        const val HOUR = 3_600_000L
        const val DAY = 24 * HOUR
    }
}
