package io.github.wladyslawpopov.kpage.demo.domain.chat

import kotlinx.serialization.Serializable

/** How the fake interlocutor of a chat behaves; chosen at random when the chat is created. */
enum class ChatScenario {
    /** Answers at once, with several messages in a row. */
    CHATTY,

    /** Comes with a long history, so the list has many pages. */
    HISTORIAN,

    /** A part of the sent messages fails. */
    FLAKY,
}

/** A chat of the list. [updatedTs] is the time of its last message, in epoch milliseconds. */
@Serializable
data class Chat(
    val id: Long,
    val title: String,
    val interlocutor: String,
    val scenario: ChatScenario,
    val lastMessage: String,
    val updatedTs: Long,
)
