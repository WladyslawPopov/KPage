package io.github.wladyslawpopov.kpage.demo.domain.chat

import kotlinx.serialization.Serializable

/** A photo of a message: a file in the app's folder, written there by the platform. */
@Serializable
data class ChatAttachment(
    val path: String,
    val width: Int,
    val height: Int,
)

/**
 * A message the server keeps. [clientId] is the local id of the outgoing message it came from, so
 * the app knows which local message it confirms. [createdTs] is in epoch milliseconds.
 */
@Serializable
data class ChatMessage(
    val id: Long,
    val chatId: Long,
    val isMine: Boolean,
    val text: String,
    val attachments: List<ChatAttachment> = emptyList(),
    val createdTs: Long,
    val clientId: String? = null,
)
