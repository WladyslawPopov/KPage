package io.github.wladyslawpopov.kpage.demo.tour

import android.graphics.Point
import android.graphics.Rect
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import io.github.wladyslawpopov.kpage.demo.uicore.testing.UiTestId
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern
import kotlin.math.abs

/**
 * The demo tour: Space (the grid, a favorite, back to the list; a search by a word, favorites in it) -> far
 * down the found images, a favorite -> its details (take the heart off, back) -> Settings (a dark theme,
 * Polish) -> back up the list, more favorites -> Settings (English again) -> Favorites, the details of one ->
 * the list of one of its tags (the filters: the tag and a year, favorites there) -> back to the details (the
 * heart off) -> Favorites (the changes are there; remove one by a swipe) -> a chat room (look through the
 * history, send a message) -> Settings (clear the cache) -> the lists again -> Settings (the system theme).
 *
 * UiAutomator in its own process: the app runs on the real clock and every touch goes through the system,
 * so the flicks glide and slow down as a person's do. Elements are found by the ids of [UiTestId], shown
 * as resource ids by the app. The tour pauses only where the viewer needs to see a result: a heart set or
 * taken off, the details or a chat room just opened or left, the filters or the settings just applied, a
 * tap that changes the screen (the layout, the search, a list by a tag); everything else goes on at once,
 * waiting only for what it needs. The data comes from NASA live: the tour
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

        // The grid: the same images in two columns, a calm glide down them, a favorite, back to the list.
        find(UiTestId.LAYOUT_TOGGLE).click()
        look(CHANGE_HOLD)
        flickUntil(up = true, GRID_FLICK_MS, GRID_GAP_MS, burst = 1) { it >= GRID_POSITION - GRID_COAST_ITEMS }
        settle()
        heartOf(cardInMiddle()).click()
        look(HEART_HOLD)
        find(UiTestId.LAYOUT_TOGGLE).click()
        look(CHANGE_HOLD)

        // Search: a word narrows the list; the check mark leaves it as a chip, and the tour goes on in it.
        find(UiTestId.SEARCH_OPEN).click()
        typeSlowly(UiTestId.SEARCH_FIELD, SEARCH)
        find(UiTestId.SEARCH_DONE).click()
        waitFor { count(UiTestId.IMAGE_CARD) > 0 }
        look(CHANGE_HOLD)
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
        look(DETAILS_HOLD)
        find(UiTestId.HEART).click()
        look(HEART_HOLD)
        device.pressBack()
        waitFor { count(UiTestId.IMAGE_CARD) > 0 }
        look(CHANGE_HOLD)

        // Settings in the middle of the way: a dark theme and Polish, applied at once; the list keeps its place.
        openTab("settings")
        find(UiTestId.option("dark")).click()
        look(SETTINGS_HOLD)
        find(UiTestId.option("polish")).click()
        look(SETTINGS_HOLD)
        openTab("space")
        waitFor { count(UiTestId.IMAGE_CARD) > 0 }

        // Back up, almost to the start of the list: a few more favorites there.
        flickUntil(up = false) { it <= NEAR_START_POSITION + COAST_ITEMS }
        settle()
        repeat(2) {
            scroll(up = true)
            heartOf(cardInMiddle()).click()
            look(HEART_HOLD)
        }

        // English again: the rest of the tour is read, not only seen.
        openTab("settings")
        find(UiTestId.option("english")).click()
        look(SETTINGS_HOLD)

        // Favorites: they are there; the details of one.
        openTab("favorites")
        waitFor { count(UiTestId.IMAGE_CARD) > 0 }
        find(UiTestId.IMAGE_CARD).click()
        waitFor { count(UiTestId.IMAGE_CARD) == 0 && count(UiTestId.HEART) > 0 }
        look(DETAILS_HOLD)

        // One of its tags opens a new list filtered by it.
        tagInDetails().click()
        waitFor { count(UiTestId.IMAGE_CARD) > 0 }
        look(CHANGE_HOLD)

        // The filters show the tag; a year narrows the list.
        find(UiTestId.FILTERS_OPEN).click()
        find(UiTestId.FILTER_YEAR_FROM)
        look(CHANGE_HOLD)
        find(UiTestId.FILTER_YEAR_FROM).click()
        pickFilterYear()
        look(CHANGE_HOLD)
        find(UiTestId.FILTERS_SHOW).click()
        waitFor { count(UiTestId.IMAGE_CARD) > 0 }
        look(FILTERS_HOLD)

        // Down the list, a heart on an image that is not a favorite yet after every scroll.
        repeat(TAG_FAVORITES) {
            scroll(up = true)
            heartOff()?.let {
                it.click()
                look(HEART_HOLD)
            }
        }

        // Back to the details, scrolled down to the tag: up to the picture, the heart off.
        device.pressBack()
        waitFor { count(UiTestId.IMAGE_CARD) == 0 }
        detailsToTop()
        takeHeartOff()
        look(HEART_HOLD)

        // Favorites: the image is gone, the new ones are there; remove one with a swipe.
        device.pressBack()
        settle()
        look(CHANGE_HOLD)
        swipeLeftOverFirstCard()
        find(UiTestId.CONFIRM).click()
        look(HEART_HOLD)

        // Chat: create a room (the app opens it), look through its history, send a message.
        openTab("chat")
        find(UiTestId.NEW_CHAT).click()
        typeSlowly(UiTestId.NEW_CHAT_TITLE, CHAT_TITLE)
        typeSlowly(UiTestId.NEW_CHAT_INTERLOCUTOR, CHAT_INTERLOCUTOR)
        find(UiTestId.NEW_CHAT_CREATE).click()
        waitFor { count(UiTestId.MESSAGE_FIELD) > 0 }
        // The room opens, then its history loads: time to see it before looking through it.
        look(CHAT_HOLD)
        repeat(2) { flick(up = false) }
        find(UiTestId.MESSAGE_FIELD).click()
        typeSlowly(UiTestId.MESSAGE_FIELD, MESSAGE)
        waitFor { device.findObject(By.res(UiTestId.SEND.id))?.isEnabled == true }
        find(UiTestId.SEND).click()
        // The message is in the list, not only in the field: the field has its own id. A look before leaving.
        waitFor { device.findObjects(By.text(MESSAGE)).any { it.resourceName != UiTestId.MESSAGE_FIELD.id } }
        look(CHAT_HOLD)
        // The room has no tab bar: back to the list of chats, the new one on top.
        backToTabs()
        look(CHAT_LIST_HOLD)

        // Settings: clear the cache; the lists load again, the favorites and the chats stay.
        openTab("settings")
        find(UiTestId.CLEAR_CACHE).click()
        find(UiTestId.CONFIRM).click()
        look(SETTINGS_HOLD)
        openTab("space")
        waitFor { count(UiTestId.IMAGE_CARD) > 0 }
        openTab("favorites")
        waitFor { count(UiTestId.IMAGE_CARD) > 0 }
        openTab("chat")
        waitFor { count(UiTestId.CHAT_ROW) > 0 }

        // The end: the system theme again, as the next run expects.
        openTab("settings")
        find(UiTestId.option("system")).click()
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

    /**
     * A heart that is not set, clear of the bars: a new favorite, not one taken off. Compose reports the
     * `selected` of the heart as checked (selected is only for tabs).
     */
    private fun heartOff(): UiObject2? {
        val height = device.displayHeight
        val band = (height * 0.2f).toInt()..(height * 0.85f).toInt()
        return device.findObjects(By.res(UiTestId.HEART.id)).firstOrNull { !it.isChecked && it.visibleCenter.y in band }
    }

    /** Back presses until the tab bar shows: the first may only hide the keyboard. */
    private fun backToTabs() {
        val tabShown = { isFullyShown(device.findObject(By.res(UiTestId.tab("chat")))) }
        repeat(MAX_BACKS) {
            if (tabShown()) return
            device.pressBack()
            waitUntil(BACK_WAIT_MS, tabShown)
        }
    }

    /** Scrolls the details down to a field or a chip that opens a list by a tag. */
    private fun tagInDetails(): UiObject2 {
        val height = device.displayHeight
        val band = (height * 0.2f).toInt()..(height * 0.85f).toInt()
        repeat(MAX_DETAILS_SCROLLS) {
            val tag = device.findObjects(By.res(UiTestId.DETAILS_TAG.id)).firstOrNull { it.visibleCenter.y in band }
            if (tag != null) return tag
            detailsScroll(up = true)
        }
        error("No tag in the details")
    }

    /**
     * Flicks the details up to the picture with the heart, each time waiting only until they stand still. It
     * stops as soon as the heart is clear of the top bar, which would take the tap: no swipe at the very top,
     * which would pull to refresh.
     */
    private fun detailsToTop() {
        val clearTop = device.displayHeight * HEART_CLEAR_TOP
        repeat(MAX_DETAILS_SCROLLS) {
            val heart = device.findObject(By.res(UiTestId.HEART.id))
            if (heart != null && isFullyShown(heart) && heart.visibleBounds.top > clearTop) return
            swipe(0.5f, 0.3f, 0.5f, 0.9f, steps(FLICK_MS))
            // Still: the heart, or a tag while the picture is above the screen, stays where it was. With
            // neither on the screen (a long description) there is nothing to wait for: the next flick comes.
            var last: Rect? = null
            var isFirst = true
            waitFor {
                val now = runCatching {
                    (device.findObject(By.res(UiTestId.HEART.id)) ?: device.findObject(By.res(UiTestId.DETAILS_TAG.id)))
                        ?.visibleBounds
                }.getOrNull()
                (!isFirst && now == last).also {
                    last = now
                    isFirst = false
                }
            }
        }
        error("No picture at the top of the details")
    }

    /** Taps the set heart of the details until it is off: a tap the screen did not take is repeated. */
    private fun takeHeartOff() {
        repeat(MAX_HEART_TAPS) {
            val heart = find(UiTestId.HEART)
            if (!heart.isChecked) return
            heart.click()
            val end = SystemClock.uptimeMillis() + HEART_WAIT_MS
            while (SystemClock.uptimeMillis() < end) {
                if (device.findObject(By.res(UiTestId.HEART.id))?.isChecked == false) return
                SystemClock.sleep(POLL_MS)
            }
        }
        error("The heart of the details stays set")
    }

    /** One swipe over the details; they have no cards, so a short pause stands for the wait of [settle]. */
    private fun detailsScroll(up: Boolean) {
        swipe(0.5f, if (up) 0.7f else 0.35f, 0.5f, if (up) 0.35f else 0.7f, SCROLL_STEPS)
        look(SETTLE)
    }

    /**
     * Picks [FILTER_YEAR] in the open menu of years, newest first: the menu scrolls down until the year shows,
     * then a look and the tap. Nodes are read by their text only: the rows are redrawn while the menu moves.
     */
    private fun pickFilterYear() {
        val menu = By.scrollable(true).hasDescendant(By.text(MENU_YEAR))
        waitFor { device.findObject(menu) != null }
        device.findObject(menu)?.scrollUntil(Direction.DOWN, Until.findObject(By.text(FILTER_YEAR)))
        var last: Point? = null
        waitFor {
            val now = menuYearCenter(FILTER_YEAR)
            (now != null && now == last).also { last = now }
        }
        val year = last ?: error("No $FILTER_YEAR in the menu")
        device.click(year.x, year.y)
    }

    /** The middle of a year of the open menu, when the whole row is shown. */
    private fun menuYearCenter(year: String): Point? =
        runCatching { device.findObject(By.text(year))?.takeIf { isFullyShown(it) }?.visibleCenter }.getOrNull()

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

    /**
     * A scroll down hides the tab bar; a short pull back shows it again. The tab is open once it is selected
     * and the screens have changed places: until then the old one may still be found.
     */
    private fun openTab(name: String) {
        val tab = UiTestId.tab(name)
        if (!isFullyShown(device.findObject(By.res(tab)))) {
            swipe(0.5f, 0.45f, 0.5f, 0.6f, PULL_STEPS)
            waitFor { isFullyShown(device.findObject(By.res(tab))) }
        }
        find(tab).click()
        waitFor { device.findObject(By.res(tab))?.isSelected == true }
        look(TAB_SWITCH_MS)
    }

    /** Types like a person: a character at a time. */
    private fun typeSlowly(id: UiTestId, text: String) {
        find(id)
        for (end in 1..text.length) {
            (device.findObject(By.res(id.id)) ?: error("No ${id.id}")).text = text.take(end)
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

    private fun find(id: UiTestId): UiObject2 = find(id.id)

    /**
     * The element, once it shows and stands still: a screen just opened draws it a moment later, and a sheet
     * or a dialog still moving would take the tap elsewhere.
     */
    private fun find(id: String): UiObject2 {
        var last: Rect? = null
        waitFor(FIND_TIMEOUT_MS) {
            val now = runCatching { device.findObject(By.res(id))?.visibleBounds }.getOrNull()
            (now != null && now == last).also { last = now }
        }
        return device.findObject(By.res(id)) ?: error("No $id")
    }

    private fun count(id: UiTestId): Int = device.findObjects(By.res(id.id)).size

    /** Waits for [condition]; on a timeout the screen is kept in [FAIL_SCREENSHOT] to see what stood in the way. */
    private fun waitFor(timeoutMillis: Long = 30_000, condition: () -> Boolean) {
        if (waitUntil(timeoutMillis, condition)) return
        device.executeShellCommand("screencap -p $FAIL_SCREENSHOT")
        error("Timed out after $timeoutMillis ms, the screen is in $FAIL_SCREENSHOT")
    }

    /** Waits for [condition], no longer than [timeoutMillis]; false when it did not come. */
    private fun waitUntil(timeoutMillis: Long, condition: () -> Boolean): Boolean {
        val end = SystemClock.uptimeMillis() + timeoutMillis
        while (!condition()) {
            if (SystemClock.uptimeMillis() >= end) return false
            SystemClock.sleep(POLL_MS)
        }
        return true
    }

    /** The pause that gives the viewer time to see a result. */
    private fun look(millis: Long) = SystemClock.sleep(millis)

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

        /**
         * The looks at a result: a heart set or taken off, the details just opened, the filters or the
         * settings just applied, a chat room just opened or left, the last screen; [CHANGE_HOLD] after the
         * other taps that change the screen (the layout, the search, a list by a tag, a year).
         */
        const val HEART_HOLD = 1500L
        const val CHANGE_HOLD = 1500L
        const val DETAILS_HOLD = 1500L
        const val FILTERS_HOLD = 1500L
        const val SETTINGS_HOLD = 1500L
        const val CHAT_HOLD = 2500L
        const val CHAT_LIST_HOLD = 1500L
        const val END_HOLD = 4000L

        /** Scrolls down the list of a tag, each with a heart on a new image. */
        const val TAG_FAVORITES = 2

        /** A cap for the back presses from the chat room to the tab bar, and the wait after each. */
        const val MAX_BACKS = 3
        const val BACK_WAIT_MS = 1500L

        /** The share of the screen the heart of the details must be below: the top bar lies over the picture. */
        const val HEART_CLEAR_TOP = 0.12f

        /** Taps on the heart of the details, and how long each waits for it to go off. */
        const val MAX_HEART_TAPS = 3
        const val HEART_WAIT_MS = 2000L

        /** A cap for a run to a position, in case the counter cannot be read. */
        const val MAX_FLICKS = 150
        val COUNTER: Pattern = Pattern.compile("""(\d+) / \d+""")

        /** The year the filters start from, a few rows down the menu of years (newest first). */
        const val FILTER_YEAR = "2018"

        /** A year of the menu: it tells the menu from the list under the sheet, whose dates are longer. */
        val MENU_YEAR: Pattern = Pattern.compile("""\d{4}""")


        /** A cap for the scrolls of the details. */
        const val MAX_DETAILS_SCROLLS = 12

        const val POLL_MS = 50L
        const val FIND_TIMEOUT_MS = 10_000L

        /** After a tab is selected: the screens change places before the next step. */
        const val TAB_SWITCH_MS = 1000L

        const val FAIL_SCREENSHOT = "/sdcard/tour-fail.png"

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

        const val SETTLE = 400L
        const val KEY_MS = 150L
    }
}
