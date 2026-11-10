package io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room

import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatAttachment

sealed interface ChatRoomIntent {
    data class TextChanged(val text: String) : ChatRoomIntent

    /** Photos the platform has picked and written into the app's folder. */
    data class AddPhotos(val photos: List<ChatAttachment>) : ChatRoomIntent

    data class RemovePhoto(val photo: ChatAttachment) : ChatRoomIntent

    /** Sends what is typed and attached. */
    data object Send : ChatRoomIntent

    /** Sends a failed message again. */
    data class Retry(val localId: String) : ChatRoomIntent

    /** Removes a failed message. */
    data class DeleteOutgoing(val localId: String) : ChatRoomIntent

    /** Deletes a message on the server. */
    data class DeleteMessage(val id: Long) : ChatRoomIntent

    /** The screen has put a text into the clipboard. */
    data object Copied : ChatRoomIntent

    /** Asks to delete the whole chat. */
    data object AskDeleteChat : ChatRoomIntent

    /** The user agreed to delete the chat; the screen closes after it. */
    data object ConfirmDeleteChat : ChatRoomIntent

    /** The user refused to delete the chat. */
    data object CancelDeleteChat : ChatRoomIntent
}
