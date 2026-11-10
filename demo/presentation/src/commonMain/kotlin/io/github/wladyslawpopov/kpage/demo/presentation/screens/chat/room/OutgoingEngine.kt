package io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room

import io.github.wladyslawpopov.kpage.core.paging.data.state.PagedList
import io.github.wladyslawpopov.kpage.core.paging.data.state.pagedListOf
import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatMessage
import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatRepository
import io.github.wladyslawpopov.kpage.demo.domain.chat.OutgoingMessage
import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentTasks
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageIntent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Local outgoing messages of a chat: tells which of them the shown server messages confirm (the
 * server message carries the local id) and deletes them once the confirmation is shown.
 */
internal class OutgoingEngine(
    private val tasks: ComponentTasks,
    private val chatId: Long,
    private val repository: ChatRepository,
    private val states: Flow<ChatRoomState>,
    private val onChanged: () -> Unit,
    private val onPageIntent: (PageIntent) -> Unit,
) {
    private var outgoing = emptyList<OutgoingMessage>()
    private var serverMessages: PagedList<ChatMessage> = pagedListOf(0)

    /** Called once every engine of the screen exists: [onChanged] may be called right away. */
    fun start() {
        tasks.launchMain {
            repository.recoverInterrupted(chatId)
            repository.observeOutgoing(chatId).collect {
                outgoing = it
                onChanged()
                resolveConfirmed()
            }
        }
        tasks.launchMain {
            states.map { it.list.items }.distinctUntilChanged().collect {
                serverMessages = it
                onChanged()
                resolveConfirmed()
            }
        }
    }

    /** Local messages that no message of [items] confirms, the newest first. */
    fun unconfirmed(items: PagedList<ChatMessage>): List<OutgoingMessage> {
        val confirmed = items.confirmedIds()
        return outgoing.filter { it.localId !in confirmed }.sortedByDescending { it.attemptedTs }
    }

    fun delete(localId: String) {
        tasks.launchMain {
            onPageIntent(PageIntent.ClearError)
            repository.deleteOutgoing(chatId, localId)
        }
    }

    // Delete only after the server replacement has been shown.
    private suspend fun resolveConfirmed() {
        val confirmed = serverMessages.confirmedIds()
        val resolved = outgoing.map { it.localId }.filter { it in confirmed }
        if (resolved.isEmpty()) return
        try {
            repository.resolveOutgoing(chatId, resolved)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            onPageIntent(PageIntent.OnError(error))
        }
    }

    private fun PagedList<ChatMessage>.confirmedIds(): Set<String> =
        loadedIndices.mapNotNullTo(HashSet()) { this[it]?.clientId }
}
