package io.github.wladyslawpopov.kpage.demo.tour

import android.graphics.Rect
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import io.github.wladyslawpopov.kpage.demo.uicore.testing.UiTestId
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern
import kotlin.math.abs

/**
 * The demo tour: Space (the grid, a favorite, back to the list; a search by a word, favorites in it) -> far
 * down the found images, a favorite -> its details
 * (take the heart off, back) -> back up the list, more favorites -> Favorites (remove one by a swipe) ->
 * a chat room (look through the history, send a message).
 *
 * UiAutomator in its own process: the app runs on the real clock and every touch goes through the system,
 * so the flicks glide and slow down as a person's do. Elements are found by the ids of [UiTestId], shown
 * as resource ids by the app. The pauses are the pace of the video. The data comes from NASA live: the tour
 * never relies on a given image. The app is cleared before the tour: empty Favorites, no chats.
 *
 * Run: `./gradlew :tour:connectedTourAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class DemoTour {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun launchClean() {
        // A search waits for 500 ms with no accessibility events, up to this; while images load and animate
        // the app is never quiet, and every search would wait for the whole timeout. The tour waits itself.
        Configurator.getInstance().waitForIdleTimeout = 0
        device.executeShellCommand("pm clear $APP")
        device.executeShellCommand("am start -W -n $APP/$ACTIVITY")
    }

    @Test
    fun tour() {
        waitFor { count(UiTestId.IMAGE_CARD) > 0 }
        look(LONG)

        // The grid: the same images in two columns, a calm glide down them, a favorite, back to the list.
        find(UiTestId.LAYOUT_TOGGLE).click()
        look(TRANSITION)
        flickUntil(up = true, GRID_FLICK_MS, GRID_GAP_MS, burst = 1) { it >= GRID_POSITION - GRID_COAST_ITEMS }
        settle()
        heartOf(cardInMiddle()).click()
        look(HEART_HOLD)
        find(UiTestId.LAYOUT_TOGGLE).click()
        look(LAYOUT_HOLD)

        // Search: a word narrows the list; the check mark leaves it as a chip, and the tour goes on in it.
        find(UiTestId.SEARCH_OPEN).click()
        waitFor { count(UiTestId.SEARCH_FIELD) > 0 }
        look()
        typeSlowly(UiTestId.SEARCH_FIELD, SEARCH)
        look()
        find(UiTestId.SEARCH_DONE).click()
        waitFor { count(UiTestId.IMAGE_CARD) > 0 }
        look(LONG)
        repeat(2) { flick(up = true, SOFT_FLICK_MS, SOFT_GAP_MS) }
        settle()
        // A favorite among what was found.
        heartOf(cardInMiddle()).click()
        look(HEART_HOLD)

        // Far down the found images, by the position counter, to show the paginator on a long list.
        flickUntil(up = true) { it >= FAR_POSITION - COAST_ITEMS }
        settle()
        val card = cardInMiddle()
        heartOf(card).click()
        look(HEART_HOLD)

        // Its details: the heart is set there too; take it off and come back, the list shows it off.
        card.click()
        waitFor { count(UiTestId.IMAGE_CARD) == 0 && count(UiTestId.HEART) > 0 }
        look(TRANSITION)
        find(UiTestId.HEART).click()
        look(LONG)
        device.pressBack()
        waitFor { count(UiTestId.IMAGE_CARD) > 0 }
        look(TRANSITION)

        // Back up, almost to the start of the list: a few more favorites there.
        flickUntil(up = false) { it <= NEAR_START_POSITION + COAST_ITEMS }
        settle()
        repeat(2) {
            scroll(up = true)
            heartOf(cardInMiddle()).click()
            look(HEART_HOLD)
        }

        // Favorites: they are there; remove one with a swipe.
        openTab("favorites")
        waitFor { count(UiTestId.IMAGE_CARD) > 0 }
        look(LONG)
        swipeLeftOverFirstCard()
        waitFor { count(UiTestId.CONFIRM) > 0 }
        look()
        find(UiTestId.CONFIRM).click()
        look(LONG)

        // Chat: create a room (the app opens it), look through its history, send a message.
        openTab("chat")
        look()
        find(UiTestId.NEW_CHAT).click()
        waitFor { count(UiTestId.NEW_CHAT_TITLE) > 0 }
        look()
        typeSlowly(UiTestId.NEW_CHAT_TITLE, CHAT_TITLE)
        typeSlowly(UiTestId.NEW_CHAT_INTERLOCUTOR, CHAT_INTERLOCUTOR)
        look()
        find(UiTestId.NEW_CHAT_CREATE).click()
        waitFor { count(UiTestId.MESSAGE_FIELD) > 0 }
        // The room opens, then its history loads: give it time to appear before looking through it.
        look(CHAT_LOAD)
        repeat(2) { flick(up = false) }
        look(GLIDE)
        find(UiTestId.MESSAGE_FIELD).click()
        look()
        typeSlowly(UiTestId.MESSAGE_FIELD, MESSAGE)
        look()
        waitFor { device.findObject(By.res(UiTestId.SEND.id))?.isEnabled == true }
        find(UiTestId.SEND).click()
        // The message is in the list, not only in the field: the field has its own id.
        waitFor { device.findObjects(By.text(MESSAGE)).any { it.resourceName != UiTestId.MESSAGE_FIELD.id } }
        look(END_HOLD)
    }

    // --- Steps -------------------------------------------------------------------------------------

    /**
     * The card nearest to the middle of the screen whose middle and heart are clear of the translucent bars.
     * A tile of the grid is too tall to lie wholly in the middle band, so only these two points count.
     */
    private fun cardInMiddle(): UiObject2 {
        val height = device.displayHeight
        val band = (height * 0.3f).toInt()..(height * 0.8f).toInt()
        val hearts = device.findObjects(By.res(UiTestId.HEART.id)).map { it.visibleCenter }
        return device.findObjects(By.res(UiTestId.IMAGE_CARD.id))
            .filter { card ->
                val bounds = card.visibleBounds
                bounds.centerY() in band && hearts.any { bounds.contains(it.x, it.y) && it.y in band }
            }
            .minByOrNull { abs(it.visibleBounds.centerY() - height / 2) }
            ?: error("No card in the middle of the screen")
    }

    /** The heart lying inside the card: the card and its heart are of the same image. */
    private fun heartOf(card: UiObject2): UiObject2 {
        val bounds = card.visibleBounds
        return device.findObjects(By.res(UiTestId.HEART.id))
            .firstOrNull { bounds.contains(it.visibleCenter.x, it.visibleCenter.y) }
            ?: error("No heart inside the card")
    }

    /** One slow swipe over the content: up shows what is further down the list, down what is above. */
    private fun scroll(up: Boolean) {
        swipe(0.5f, if (up) 0.75f else 0.3f, 0.5f, if (up) 0.3f else 0.75f, SCROLL_STEPS)
        settle()
    }

    /** Waits until the list stands still, no longer than it takes: a card is where it was a moment ago. */
    private fun settle() {
        var last: Rect? = null
        waitFor {
            // A card gone between the search and the read is a list still moving.
            val now = runCatching { device.findObject(By.res(UiTestId.IMAGE_CARD.id))?.visibleBounds }.getOrNull()
            (now != null && now == last).also { last = now }
        }
    }

    /** A swipe of the first card to the left, the removal gesture. */
    private fun swipeLeftOverFirstCard() {
        val bounds = find(UiTestId.IMAGE_CARD).visibleBounds
        val y = bounds.centerY()
        device.swipe(
            bounds.left + (bounds.width() * 0.9f).toInt(), y,
            bounds.left + (bounds.width() * 0.1f).toInt(), y,
            CARD_SWIPE_STEPS,
        )
    }

    /** A scroll down hides the tab bar; a short pull back shows it again. */
    private fun openTab(name: String) {
        val tab = UiTestId.tab(name)
        if (!isFullyShown(device.findObject(By.res(tab)))) {
            swipe(0.5f, 0.45f, 0.5f, 0.6f, PULL_STEPS)
            look(SETTLE)
        }
        device.findObject(By.res(tab))?.click() ?: error("No tab $tab")
        look(TRANSITION)
    }

    /** Types like a person: a character at a time. */
    private fun typeSlowly(id: UiTestId, text: String) {
        for (end in 1..text.length) {
            find(id).text = text.take(end)
            look(KEY_MS)
        }
    }

    // --- Flicks ------------------------------------------------------------------------------------

    /** A flick: the list goes on by itself after it and slows down. The next one comes while it still goes. */
    private fun flick(up: Boolean, durationMillis: Long = FLICK_MS, gapMillis: Long = FLICK_GAP_MS) {
        // A flick down starts below the header and its chips: a touch there does not move the list.
        swipe(0.5f, if (up) 0.85f else 0.3f, 0.5f, if (up) 0.15f else 0.9f, steps(durationMillis))
        look(gapMillis)
    }

    /** The "last visible / total" counter of the list: the number the viewer sees on the screen. */
    private fun position(): Int? = device.findObjects(By.text(COUNTER))
        .mapNotNull { COUNTER.matcher(it.text ?: "").takeIf { m -> m.matches() }?.group(1)?.toIntOrNull() }
        .maxOrNull()

    /**
     * A run far along the list: flicks one right after another, faster than a glide, until the counter says
     * [reached]. The counter is read between bursts: a read takes time and would slow the run down. An
     * unreadable counter does not stop the tour, the cap does; a counter that stands still (the end of a
     * shorter list) stops the run too.
     */
    private fun flickUntil(
        up: Boolean,
        flickMillis: Long = RUN_FLICK_MS,
        gapMillis: Long = RUN_GAP_MS,
        burst: Int = RUN_BURST,
        reached: (position: Int) -> Boolean,
    ) {
        var last: Int? = null
        repeat(MAX_FLICKS / burst) {
            val now = position()
            if (now?.let(reached) == true) return
            // The counter stands still: the list ends before the goal.
            if (now != null && now == last) return
            last = now
            repeat(burst) { flick(up, flickMillis, gapMillis) }
        }
    }

    // --- Touch and waiting -------------------------------------------------------------------------

    /**
     * A finger from one share of the screen to another in [steps] moves: few moves are a fast swipe, let go
     * at speed. A move is sent and waited for, so it takes 5 ms or more: a long swipe takes longer than
     * its steps count says.
     */
    private fun swipe(fromX: Float, fromY: Float, toX: Float, toY: Float, steps: Int) {
        val width = device.displayWidth
        val height = device.displayHeight
        device.swipe(
            (width * fromX).toInt(), (height * fromY).toInt(),
            (width * toX).toInt(), (height * toY).toInt(),
            steps,
        )
    }

    /** `UiDevice.swipe` takes at least 5 ms a step: for the short flicks this is close enough. */
    private fun steps(durationMillis: Long): Int = (durationMillis / STEP_MS).toInt().coerceAtLeast(2)

    private fun isFullyShown(node: UiObject2?): Boolean {
        if (node == null) return false
        val visible: Rect = node.visibleBounds
        return visible.height() > 0 && visible.bottom <= device.displayHeight
    }

    private fun find(id: UiTestId): UiObject2 = device.findObject(By.res(id.id)) ?: error("No ${id.id}")

    private fun count(id: UiTestId): Int = device.findObjects(By.res(id.id)).size

    private fun waitFor(timeoutMillis: Long = 30_000, condition: () -> Boolean) {
        val end = SystemClock.uptimeMillis() + timeoutMillis
        while (!condition()) {
            check(SystemClock.uptimeMillis() < end) { "Timed out after $timeoutMillis ms" }
            SystemClock.sleep(POLL_MS)
        }
    }

    /** The pause that gives the viewer time to see the screen. */
    private fun look(millis: Long = SHORT) = SystemClock.sleep(millis)

    private companion object {
        const val APP = "io.github.wladyslawpopov.kpage.demo"
        const val ACTIVITY = "io.github.wladyslawpopov.kpage.demo.android.MainActivity"

        const val MESSAGE = "Hello from KPage!"
        const val CHAT_TITLE = "Demo chat"
        const val CHAT_INTERLOCUTOR = "Neil"
        const val SEARCH = "Titan"

        /** The moves of the slower swipes: a scroll over the content, the removal of a card, a pull back. */
        const val SCROLL_STEPS = 40
        const val CARD_SWIPE_STEPS = 40
        const val PULL_STEPS = 20
        const val STEP_MS = 5L

        /** The counter value to flick down to, and the one to come back up to: almost the start. */
        const val FAR_POSITION = 1000
        const val NEAR_START_POSITION = 120

        /** The counter value to glide down to in the grid, and how far short of it the flicks stop. */
        const val GRID_POSITION = 150
        const val GRID_COAST_ITEMS = 40

        /** The grid's flicks: quicker than [SOFT_FLICK_MS] ones, still with a glide between them. */
        const val GRID_FLICK_MS = 45L
        const val GRID_GAP_MS = 300L

        /** A calm flick: the glide plays out for a while before the next one. */
        const val SOFT_FLICK_MS = 100L
        const val SOFT_GAP_MS = 700L

        /** After the grid turns back into the list: time to see it. */
        const val LAYOUT_HOLD = 2000L

        /** A short look at a heart just set. */
        const val HEART_HOLD = 1000L

        /** A cap for a run to a position, in case the counter cannot be read. */
        const val MAX_FLICKS = 150
        val COUNTER: Pattern = Pattern.compile("""(\d+) / \d+""")

        /** The room shows its history some time after it opens; the end of the tour stays on screen. */
        const val CHAT_LOAD = 2000L
        const val END_HOLD = 4000L

        const val POLL_MS = 50L

        /**
         * A short sharp flick, let go at about the top fling speed, and the flicks come while the glide is
         * still quick, one after another. Reading the counter between them takes time of its own.
         */
        const val FLICK_MS = 40L
        const val FLICK_GAP_MS = 100L

        /** A run far along the list: sharper flicks with almost no gap, the counter read every few. */
        const val RUN_FLICK_MS = 25L
        const val RUN_GAP_MS = 20L
        const val RUN_BURST = 3

        /** The run stops flicking this many items short of its goal: the list rolls on by itself. */
        const val COAST_ITEMS = 250

        /** The last glide, before the run turns back. */
        const val GLIDE = 2500L
        const val SETTLE = 400L
        const val KEY_MS = 150L
        const val SHORT = 600L
        const val LONG = 1000L

        /** After a screen change: the animation and the first load are over before the next step. */
        const val TRANSITION = 1200L
    }
}
