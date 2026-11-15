package io.github.wladyslawpopov.kpage.demo.uicore.testing

/**
 * Ids of the elements the demo tours find (`demo/tour` on Android, `iosAppUITests` on iOS): a Compose
 * `testTag` and a SwiftUI `accessibilityIdentifier` with the same [id], so both tours tell the same story.
 * Placeholders get no id: a flow finds only loaded rows.
 */
enum class UiTestId(val id: String) {
    /** A card of a Space image, in the list, the grid and Favorites. */
    IMAGE_CARD("image_card"),
    HEART("heart"),
    CHAT_ROW("chat_row"),
    NEW_CHAT("new_chat"),
    NEW_CHAT_TITLE("new_chat_title"),
    NEW_CHAT_INTERLOCUTOR("new_chat_interlocutor"),
    NEW_CHAT_CREATE("new_chat_create"),
    MESSAGE_FIELD("message_field"),
    SEND("send"),

    /** The confirm button of a question dialog. */
    CONFIRM("confirm"),

    /** The button that switches the Space images between the list and the grid. */
    LAYOUT_TOGGLE("layout_toggle"),

    /** The search of a top bar: the button that opens it, the field, the check mark that closes it. */
    SEARCH_OPEN("search_open"),
    SEARCH_FIELD("search_field"),
    SEARCH_DONE("search_done"),

    /** The "last visible / total" counter of a paged list. */
    POSITION("position");

    companion object {
        /** A tab of the tab bar, by the name of its `AppTab`, for example `tab_space`. */
        fun tab(name: String): String = "tab_${name.lowercase()}"
    }
}
