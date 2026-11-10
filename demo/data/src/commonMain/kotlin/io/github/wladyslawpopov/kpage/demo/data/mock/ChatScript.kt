package io.github.wladyslawpopov.kpage.demo.data.mock

import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatScenario
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/** A message of the history a chat is created with; [age] is how long before the creation it was written. */
internal class SeededMessage(val isMine: Boolean, val text: String, val age: Duration)

/** An answer of the interlocutor, written [delay] after the previous one. */
internal class ScriptReply(val delay: Duration, val text: String)

/** The behavior of the fake interlocutor of a chat. All randomness comes from the given [Random]. */
internal interface ChatScript {
    /** Share of the sent messages the server refuses, 0..1. */
    val failureRate: Float get() = 0f

    /** The history the chat starts with, oldest first. */
    fun history(random: Random): List<SeededMessage>

    fun replies(text: String, random: Random): List<ScriptReply>
}

internal fun ChatScenario.script(): ChatScript = when (this) {
    ChatScenario.CHATTY -> ChattyScript
    ChatScenario.HISTORIAN -> HistorianScript
    ChatScenario.FLAKY -> FlakyScript
}

private object ChattyScript : ChatScript {
    override fun history(random: Random) = conversation(12, step = 4.minutes, random)

    override fun replies(text: String, random: Random) =
        List(random.nextInt(2, 5)) { ScriptReply(random.nextLong(300, 900).milliseconds, PHRASES.random(random)) }
}

private object HistorianScript : ChatScript {
    // Several hundred messages over two months: the list has many pages and many dates.
    override fun history(random: Random) = conversation(400, step = 4.hours, random)

    override fun replies(text: String, random: Random) =
        listOf(ScriptReply(1200.milliseconds, "\"$text\" — noted. ${PHRASES.random(random)}"))
}

private object FlakyScript : ChatScript {
    override val failureRate = 0.4f

    override fun history(random: Random) = conversation(20, step = 30.minutes, random)

    override fun replies(text: String, random: Random) =
        listOf(ScriptReply(1000.milliseconds, PHRASES.random(random)))
}

private fun conversation(size: Int, step: Duration, random: Random): List<SeededMessage> =
    List(size) { index ->
        SeededMessage(
            isMine = random.nextInt(3) == 0,
            text = PHRASES.random(random),
            age = step * (size - index),
        )
    }

private val PHRASES = listOf(
    "Did you see the new images from the telescope?",
    "I think the launch was moved to next week.",
    "That nebula looks unreal.",
    "Send me the one with the rings.",
    "How far is that from here?",
    "The crew is back on the station.",
    "I still can't believe the size of it.",
    "Look at the shadow on the crater.",
    "They found water ice there.",
    "Let's watch the landing together.",
    "Is that a real photo or a render?",
    "The colors come from different filters.",
    "Okay.",
    "Agreed!",
    "Give me a minute.",
    "This one was taken in 1969.",
)
