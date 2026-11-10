package io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.listing

import io.github.wladyslawpopov.kpage.core.paging.data.state.LoadState
import io.github.wladyslawpopov.kpage.demo.domain.chat.Chat
import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatRepository
import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentTasks
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageEmptyState
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageIntent
import io.github.wladyslawpopov.kpage.demo.presentation.paginator.PaginatorEngine
import io.github.wladyslawpopov.kpage.demo.presentation.paginator.PaginatorIntent
import io.github.wladyslawpopov.kpage.demo.presentation.paginator.PaginatorUiState
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Business scenarios of the list of chats. */
internal class ChatsEngine(
    private val tasks: ComponentTasks,
    private val repository: ChatRepository,
    private val getState: () -> ChatsState,
    private val updateState: ((ChatsState) -> ChatsState) -> Unit,
    private val onPageIntent: (PageIntent) -> Unit,
    private val onCreated: (Chat) -> Unit,
) {
    private val paginator = PaginatorEngine<Unit, Chat, Nothing>(
        getState = { getState().list },
        updateState = ::setList,
        onError = { onPageIntent(PageIntent.OnError(it)) },
    )

    // Removals and creations are applied one after another, in the order they were asked for.
    private val changeLock = Mutex()

    fun start() {
        tasks.launchMain {
            paginator.collectPaging(flowOf(Unit)) { _, scope -> repository.chats(scope) }
        }
        // Any change of a chat reaches the list here: a new message moves its chat to the top.
        tasks.launchMain {
            repository.changes.collect { paginator.onIntent(PaginatorIntent.Refresh) }
        }
    }

    fun onIntent(intent: ChatsIntent) {
        when (intent) {
            is ChatsIntent.Click -> if (getState().isSelecting) toggle(intent.id)
            is ChatsIntent.LongClick -> toggle(intent.id)
            ChatsIntent.ClearSelection -> updateState { it.copy(selected = emptySet()) }
            ChatsIntent.DeleteSelected -> askToDelete(getState().selected)
            is ChatsIntent.DeleteOne -> askToDelete(setOf(intent.id))
            ChatsIntent.ConfirmDelete -> confirmDelete()
            ChatsIntent.CancelDelete -> updateState { it.copy(pendingDelete = emptySet()) }
            ChatsIntent.StartCreate -> updateState { it.copy(isCreating = true) }
            ChatsIntent.CancelCreate -> updateState { it.copy(isCreating = false) }
            is ChatsIntent.Create -> create(intent.title, intent.interlocutor)
        }
    }

    fun onRefresh() = paginator.onIntent(PaginatorIntent.Refresh)

    private fun toggle(id: Long) =
        updateState { state ->
            state.copy(selected = if (id in state.selected) state.selected - id else state.selected + id)
        }

    private fun askToDelete(ids: Set<Long>) {
        if (ids.isNotEmpty()) updateState { it.copy(pendingDelete = ids) }
    }

    // The list renews itself on the change the repository reports.
    private fun confirmDelete() {
        val ids = getState().pendingDelete
        if (ids.isEmpty()) return
        updateState { it.copy(pendingDelete = emptySet(), selected = it.selected - ids) }
        tasks.launchMain {
            changeLock.withLock { repository.deleteChats(ids) }
        }
    }

    private fun create(title: String, interlocutor: String) {
        val chatTitle = title.trim().take(MAX_NAME_LENGTH)
        val name = interlocutor.trim().take(MAX_NAME_LENGTH)
        if (chatTitle.isEmpty() || name.isEmpty()) return
        updateState { it.copy(isCreating = false) }
        tasks.launchMain {
            val chat = changeLock.withLock { repository.createChat(chatTitle, name) }
            onCreated(chat)
        }
    }

    private fun setList(list: PaginatorUiState<Unit, Chat, Nothing>) {
        updateState { state ->
            state.copy(
                list = list,
                page = state.page.copy(
                    isLoading = list.loadState is LoadState.INITIAL,
                    empty = if (list.isEmptyResult) {
                        PageEmptyState(
                            title = AppStringKey.Chat.EMPTY,
                            icon = AppIconKey.CHAT,
                            actionText = AppStringKey.Chat.CREATE,
                            action = PageIntent.EmptyAction,
                        )
                    } else {
                        null
                    },
                ),
            )
        }
    }

    private companion object {
        const val MAX_NAME_LENGTH = 40
    }
}
