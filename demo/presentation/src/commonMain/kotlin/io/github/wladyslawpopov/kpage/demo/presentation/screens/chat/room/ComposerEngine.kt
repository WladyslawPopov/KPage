package io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room

import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatAttachment
import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatDraft
import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatRepository
import io.github.wladyslawpopov.kpage.demo.domain.chat.OutgoingMessage
import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentTasks
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageIntent
import io.github.wladyslawpopov.kpage.demo.presentation.time.nowMillis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * The field of the room: the draft kept in the database and the attempts to send. A message is saved
 * locally before it is sent, so it survives a failure and a restart; its local id goes to the server,
 * which makes a repeated send harmless.
 */
internal class ComposerEngine(
    private val tasks: ComponentTasks,
    private val chatId: Long,
    private val repository: ChatRepository,
    private val getState: () -> ChatRoomState,
    private val updateState: ((ChatRoomState) -> ChatRoomState) -> Unit,
    private val onPageIntent: (PageIntent) -> Unit,
) {
    private var sendJob: Job? = null
    private val draftLock = Mutex()
    private val draft = MutableStateFlow(snapshotDraft())

    // The draft may also be written from elsewhere; the final save is only for a draft changed here.
    private var changedHere = false

    private val restoreJob = tasks.launchMain {
        repository.draft(chatId)?.let { saved ->
            updateState { it.copy(text = saved.text, attachments = saved.attachments) }
        }
        draft.value = snapshotDraft()
    }

    fun start() {
        tasks.launchMain {
            restoreJob.join()
            try {
                draft.collect { value ->
                    if (!changedHere) return@collect
                    try {
                        draftLock.withLock { repository.saveDraft(value) }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Throwable) {
                        onPageIntent(PageIntent.OnError(error))
                    }
                }
            } finally {
                if (changedHere) {
                    withContext(NonCancellable) {
                        draftLock.withLock { repository.saveDraft(draft.value) }
                    }
                }
            }
        }
    }

    fun onIntent(intent: ChatRoomIntent) {
        if (!tasks.isActive) return
        when (intent) {
            is ChatRoomIntent.TextChanged -> edit { it.copy(text = intent.text) }
            is ChatRoomIntent.AddPhotos -> edit {
                val free = ChatRoomState.MAX_ATTACHMENTS - it.attachments.size
                it.copy(attachments = it.attachments + intent.photos.take(free.coerceAtLeast(0)))
            }

            is ChatRoomIntent.RemovePhoto -> edit { it.copy(attachments = it.attachments - intent.photo) }
            ChatRoomIntent.Send -> send()
            is ChatRoomIntent.Retry -> retry(intent.localId)
            else -> Unit
        }
    }

    private fun edit(transform: (ChatRoomState) -> ChatRoomState) {
        changedHere = true
        updateState(transform)
        draft.value = snapshotDraft()
    }

    private fun snapshotDraft(): ChatDraft {
        val state = getState()
        return ChatDraft(chatId, state.text, state.attachments, nowMillis())
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun send() {
        if (!getState().canSend || sendJob?.isActive == true) return
        onPageIntent(PageIntent.ClearError)
        sendJob = tasks.launchMain {
            restoreJob.join()
            val message = draftLock.withLock {
                val content = snapshotDraft()
                if (content.text.isBlank() && content.attachments.isEmpty()) return@launchMain
                val message = OutgoingMessage(
                    localId = Uuid.random().toString(),
                    chatId = chatId,
                    text = content.text.trim(),
                    attachments = content.attachments,
                    createdTs = nowMillis(),
                )
                // Saved together with the cleared draft: a cancel here must not lose the message.
                withContext(NonCancellable) {
                    repository.stageOutgoing(message)
                    updateState { it.copy(text = "", attachments = emptyList<ChatAttachment>()) }
                    draft.value = snapshotDraft()
                }
                message
            }
            performSend(message)
        }
    }

    private fun retry(localId: String) {
        if (sendJob?.isActive == true) return
        onPageIntent(PageIntent.ClearError)
        sendJob = tasks.launchMain {
            val message = repository.retryOutgoing(chatId, localId, nowMillis()) ?: return@launchMain
            performSend(message)
        }
    }

    // A failed send stays in the list as "not sent", with a retry in its menu.
    private suspend fun performSend(message: OutgoingMessage) {
        try {
            repository.send(message)
        } catch (error: Throwable) {
            withContext(NonCancellable) { repository.finishAttempt(chatId, message.localId, failed = true) }
            if (error is CancellationException) throw error
            onPageIntent(PageIntent.OnError(error))
            return
        }
        repository.finishAttempt(chatId, message.localId, failed = false)
    }
}
