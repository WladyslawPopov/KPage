package io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.listing

import io.github.wladyslawpopov.kpage.demo.domain.cache.CacheRepository
import io.github.wladyslawpopov.kpage.demo.domain.cache.CacheStats
import io.github.wladyslawpopov.kpage.demo.domain.chat.Chat
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageIntent
import io.github.wladyslawpopov.kpage.demo.presentation.testing.FakeChatRepository
import io.github.wladyslawpopov.kpage.demo.presentation.testing.TestTasks
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChatsEngineTest {

    private class FakeCache : CacheRepository {
        override val cleared = MutableSharedFlow<Unit>()
        override suspend fun stats() = CacheStats(0, 0)
        override suspend fun clear() = Unit
    }

    private class Harness(scope: TestScope) {
        val chats = FakeChatRepository()
        val cache = FakeCache()
        val intents = mutableListOf<PageIntent>()
        val opened = mutableListOf<Chat>()
        var state = ChatsState()
        val engine = ChatsEngine(
            tasks = TestTasks(
                scope.backgroundScope,
                StandardTestDispatcher(scope.testScheduler),
                StandardTestDispatcher(scope.testScheduler),
            ),
            repository = chats,
            cache = cache,
            getState = { state },
            updateState = { state = it(state) },
            onPageIntent = { intents += it },
            onCreated = { opened += it },
        )
    }

    private fun TestScope.started() = Harness(this).also {
        it.engine.start()
        runCurrent()
    }

    @Test
    fun a_change_of_any_chat_and_a_cleared_cache_refresh_the_list() = runTest {
        val harness = started()

        harness.chats.changes.emit(7)
        runCurrent()
        assertEquals(1, harness.chats.chatsPaginator.refreshCount)

        harness.cache.cleared.emit(Unit)
        runCurrent()
        assertEquals(2, harness.chats.chatsPaginator.refreshCount)
    }

    @Test
    fun a_tap_selects_only_after_a_long_press_started_the_selection() = runTest {
        val harness = started()

        harness.engine.onIntent(ChatsIntent.Click(1))
        assertTrue(harness.state.selected.isEmpty())

        harness.engine.onIntent(ChatsIntent.LongClick(1))
        harness.engine.onIntent(ChatsIntent.Click(2))
        assertEquals(setOf(1L, 2L), harness.state.selected)

        harness.engine.onIntent(ChatsIntent.Click(1))
        assertEquals(setOf(2L), harness.state.selected)

        harness.engine.onIntent(ChatsIntent.ClearSelection)
        assertFalse(harness.state.isSelecting)
    }

    @Test
    fun deleting_asks_first_and_deletes_nothing_until_confirmed() = runTest {
        val harness = started()
        harness.engine.onIntent(ChatsIntent.LongClick(1))
        harness.engine.onIntent(ChatsIntent.LongClick(2))

        harness.engine.onIntent(ChatsIntent.DeleteSelected)
        runCurrent()
        assertEquals(setOf(1L, 2L), harness.state.pendingDelete)
        assertTrue(harness.chats.deletedChats.isEmpty())

        harness.engine.onIntent(ChatsIntent.CancelDelete)
        runCurrent()
        assertTrue(harness.state.pendingDelete.isEmpty())
        assertTrue(harness.chats.deletedChats.isEmpty())
    }

    @Test
    fun deleting_with_nothing_chosen_asks_nothing() = runTest {
        val harness = started()

        harness.engine.onIntent(ChatsIntent.DeleteSelected)

        assertTrue(harness.state.pendingDelete.isEmpty())
    }

    @Test
    fun a_confirmed_delete_removes_the_chats_and_takes_them_out_of_the_selection() = runTest {
        val harness = started()
        harness.engine.onIntent(ChatsIntent.LongClick(1))
        harness.engine.onIntent(ChatsIntent.LongClick(2))
        harness.engine.onIntent(ChatsIntent.DeleteOne(1))

        harness.engine.onIntent(ChatsIntent.ConfirmDelete)
        runCurrent()

        assertEquals(listOf<Collection<Long>>(setOf(1L)), harness.chats.deletedChats)
        assertTrue(harness.state.pendingDelete.isEmpty())
        assertEquals(setOf(2L), harness.state.selected)
    }

    @Test
    fun the_new_chat_is_trimmed_cut_to_forty_characters_and_opened() = runTest {
        val harness = started()
        harness.engine.onIntent(ChatsIntent.StartCreate)
        assertTrue(harness.state.isCreating)

        harness.engine.onIntent(ChatsIntent.Create("  Title  ", "x".repeat(60)))
        runCurrent()

        val chat = harness.chats.createdChats.single()
        assertEquals("Title", chat.title)
        assertEquals("x".repeat(40), chat.interlocutor)
        assertFalse(harness.state.isCreating)
        assertEquals(listOf(chat), harness.opened)
    }

    @Test
    fun a_blank_title_or_name_creates_nothing_and_keeps_the_dialog() = runTest {
        val harness = started()
        harness.engine.onIntent(ChatsIntent.StartCreate)

        harness.engine.onIntent(ChatsIntent.Create("   ", "Bot"))
        harness.engine.onIntent(ChatsIntent.Create("Title", ""))
        runCurrent()

        assertTrue(harness.chats.createdChats.isEmpty())
        assertTrue(harness.state.isCreating)

        harness.engine.onIntent(ChatsIntent.CancelCreate)
        assertFalse(harness.state.isCreating)
    }

    @Test
    fun changes_are_applied_in_the_order_they_were_asked_for() = runTest {
        val harness = started()
        harness.chats.createGate = CompletableDeferred()
        harness.engine.onIntent(ChatsIntent.Create("Title", "Bot"))
        runCurrent()
        harness.engine.onIntent(ChatsIntent.DeleteOne(1))
        harness.engine.onIntent(ChatsIntent.ConfirmDelete)
        runCurrent()

        assertTrue(harness.chats.deletedChats.isEmpty(), "the delete waits for the creation")

        harness.chats.createGate?.complete(Unit)
        runCurrent()

        assertEquals(1, harness.chats.createdChats.size)
        assertEquals(listOf<Collection<Long>>(setOf(1L)), harness.chats.deletedChats)
    }

    @Test
    fun an_empty_list_offers_to_create_a_chat() = runTest {
        val harness = started()
        assertNull(harness.state.page.empty)

        harness.chats.chatsPaginator.recover()
        runCurrent()

        assertEquals(PageIntent.EmptyAction, harness.state.page.empty?.action)
        assertNotNull(harness.state.page.empty?.actionText)
    }
}
