package io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room

import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatAttachment
import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatDraft
import io.github.wladyslawpopov.kpage.demo.domain.chat.OutgoingMessage
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageIntent
import io.github.wladyslawpopov.kpage.demo.presentation.paginator.PaginatorUiState
import io.github.wladyslawpopov.kpage.demo.presentation.testing.FakeChatRepository
import io.github.wladyslawpopov.kpage.demo.presentation.testing.TestTasks
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ComposerEngineTest {

    private class Harness(scope: TestScope, saved: ChatDraft? = null) {
        val chats = FakeChatRepository().also { it.savedDraft = saved }
        val intents = mutableListOf<PageIntent>()
        var state = ChatRoomState(list = PaginatorUiState(CHAT_ID))
        val engine = ComposerEngine(
            tasks = TestTasks(
                scope.backgroundScope,
                StandardTestDispatcher(scope.testScheduler),
                StandardTestDispatcher(scope.testScheduler),
            ),
            chatId = CHAT_ID,
            repository = chats,
            getState = { state },
            updateState = { state = it(state) },
            onPageIntent = { intents += it },
        )

        fun type(text: String) = engine.onIntent(ChatRoomIntent.TextChanged(text))
        fun send() = engine.onIntent(ChatRoomIntent.Send)
    }

    private fun TestScope.started(saved: ChatDraft? = null) = Harness(this, saved).also {
        it.engine.start()
        runCurrent()
    }

    private fun photo(n: Int) = ChatAttachment(path = "/photos/$n.jpg", width = 100, height = 100)

    @Test
    fun the_saved_draft_is_restored_into_the_field() = runTest {
        val harness = started(ChatDraft(CHAT_ID, "unsent", listOf(photo(1))))

        assertEquals("unsent", harness.state.text)
        assertEquals(listOf(photo(1)), harness.state.attachments)
        // Restoring is not an edit: nothing is written back.
        assertTrue(harness.chats.drafts.isEmpty())
    }

    @Test
    fun typing_saves_the_draft() = runTest {
        val harness = started()

        harness.type("hi")
        runCurrent()

        assertEquals("hi", harness.chats.drafts.last().text)
    }

    @Test
    fun a_sent_message_is_staged_before_it_is_sent_and_the_field_is_cleared() = runTest {
        val harness = started()
        harness.type("  hello  ")
        runCurrent()

        harness.send()
        runCurrent()

        assertEquals(listOf("stage", "send", "finish:ok"), harness.chats.events)
        val message = harness.chats.sent.single()
        assertEquals("hello", message.text)
        // The server gets the id of the staged message: a repeated send makes one message.
        assertEquals(harness.chats.staged.single().localId, message.localId)
        assertEquals("", harness.state.text)
        assertTrue(harness.chats.drafts.last().isEmpty)
    }

    @Test
    fun a_blank_field_sends_nothing() = runTest {
        val harness = started()
        harness.type("   ")

        harness.send()
        runCurrent()

        assertTrue(harness.chats.events.isEmpty())
    }

    @Test
    fun a_second_send_is_ignored_while_the_first_is_running() = runTest {
        val harness = started()
        harness.chats.sendGate = CompletableDeferred()
        harness.type("one")
        harness.send()
        runCurrent()

        harness.type("two")
        harness.send()
        runCurrent()
        assertEquals(1, harness.chats.sent.size)

        harness.chats.sendGate?.complete(Unit)
        runCurrent()
        assertEquals(listOf("one"), harness.chats.sent.map { it.text })
        // What was typed meanwhile stays in the field.
        assertEquals("two", harness.state.text)
    }

    @Test
    fun a_failed_send_stays_staged_and_reports_the_error() = runTest {
        val harness = started()
        val error = IllegalStateException("offline")
        harness.chats.sendError = error
        harness.type("hello")

        harness.send()
        runCurrent()

        assertEquals(listOf("stage", "send", "finish:failed"), harness.chats.events)
        assertEquals(PageIntent.OnError(error), harness.intents.last())
        assertEquals("", harness.state.text)
    }

    @Test
    fun a_retry_sends_the_message_again_and_clears_the_old_error() = runTest {
        val harness = started()
        val message = OutgoingMessage(localId = "L1", chatId = CHAT_ID, text = "hello", attachments = emptyList(), createdTs = 1)
        harness.chats.retryResult = message

        harness.engine.onIntent(ChatRoomIntent.Retry("L1"))
        runCurrent()

        assertEquals(listOf("retry", "send", "finish:ok"), harness.chats.events)
        assertEquals("L1", harness.chats.sent.single().localId)
        assertEquals(PageIntent.ClearError, harness.intents.first())
    }

    @Test
    fun a_retry_of_a_message_that_cannot_be_retried_sends_nothing() = runTest {
        val harness = started()
        harness.chats.retryResult = null

        harness.engine.onIntent(ChatRoomIntent.Retry("L1"))
        runCurrent()

        assertEquals(listOf("retry"), harness.chats.events)
    }

    @Test
    fun a_message_takes_at_most_eight_photos() = runTest {
        val harness = started()

        harness.engine.onIntent(ChatRoomIntent.AddPhotos((1..6).map(::photo)))
        harness.engine.onIntent(ChatRoomIntent.AddPhotos((7..10).map(::photo)))

        assertEquals((1..8).map(::photo), harness.state.attachments)
        assertTrue(!harness.state.canAttach)

        harness.engine.onIntent(ChatRoomIntent.RemovePhoto(photo(3)))
        assertTrue(harness.state.canAttach)
    }

    @Test
    fun photos_alone_are_enough_to_send() = runTest {
        val harness = started()
        harness.engine.onIntent(ChatRoomIntent.AddPhotos(listOf(photo(1))))

        harness.send()
        runCurrent()

        assertEquals(listOf(photo(1)), harness.chats.sent.single().attachments)
        assertEquals("", harness.chats.sent.single().text)
    }

    private companion object {
        const val CHAT_ID = 1L
    }
}
