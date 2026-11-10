package io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.listing

sealed interface ChatsIntent {
    /** A tap on a chat: opens it, or chooses it while the screen is in the selection mode. */
    data class Click(val id: Long) : ChatsIntent

    /** A long press on a chat: chooses it, or takes the choice back. */
    data class LongClick(val id: Long) : ChatsIntent

    /** Leaves the selection mode. */
    data object ClearSelection : ChatsIntent

    /** Asks to delete the chosen chats. */
    data object DeleteSelected : ChatsIntent

    /** Asks to delete one chat, for example after a swipe. */
    data class DeleteOne(val id: Long) : ChatsIntent

    /** The user agreed to the removal that was asked for. */
    data object ConfirmDelete : ChatsIntent

    /** The user refused the removal that was asked for. */
    data object CancelDelete : ChatsIntent

    /** Shows the dialog of a new chat. */
    data object StartCreate : ChatsIntent

    /** Closes the dialog of a new chat without creating one. */
    data object CancelCreate : ChatsIntent

    /** Creates the chat the user described in the dialog. */
    data class Create(val title: String, val interlocutor: String) : ChatsIntent
}
