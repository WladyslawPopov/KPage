package io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.listing.mapper

import io.github.wladyslawpopov.kpage.demo.domain.chat.Chat
import io.github.wladyslawpopov.kpage.demo.presentation.time.localDay
import io.github.wladyslawpopov.kpage.demo.presentation.time.nowMillis
import io.github.wladyslawpopov.kpage.demo.uicore.text.UiText

/** The first letter of the interlocutor's name on one of [AVATAR_COLORS] colors of the platform's palette. */
data class ChatAvatar(val letter: String, val color: Int)

/** The same name always gets the same color. */
fun Chat.avatar(): ChatAvatar = ChatAvatar(
    letter = interlocutor.take(1).uppercase(),
    color = interlocutor.hashCode().mod(AVATAR_COLORS),
)

/** When the chat got its last message: the time for today, the day for earlier. */
fun Chat.time(): UiText =
    if (localDay(updatedTs) == localDay(nowMillis())) UiText.Time(updatedTs) else UiText.Date(updatedTs)

const val AVATAR_COLORS = 6
