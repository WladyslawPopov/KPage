package io.github.wladyslawpopov.kpage.demo.uicore.testing

/**
 * Ids of the elements the UI flows find (`demo/maestro`): a Compose `testTag` on Android and a SwiftUI
 * `accessibilityIdentifier` on iOS, with the same [id], so one flow runs on both platforms.
 * Placeholders get no id: a flow finds only loaded rows.
 */
enum class UiTestId(val id: String) {
    /** A card of a Space image, in the list, the grid and Favorites. */
    IMAGE_CARD("image_card"),
    HEART("heart"),
    CHAT_ROW("chat_row"),
    MESSAGE_FIELD("message_field"),
    SEND("send");

    companion object {
        /** A tab of the tab bar, by the name of its `AppTab`, for example `tab_space`. */
        fun tab(name: String): String = "tab_${name.lowercase()}"
    }
}
