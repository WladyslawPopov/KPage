package io.github.wladyslawpopov.kpage.demo.uicore.text

/** Interface strings; the app layer turns them into text. */
sealed interface AppStringKey {
    enum class Core : AppStringKey {
        OK, CANCEL, RETRY, REFRESH, CLOSE, BACK,
        OOPS_TITLE, NO_INTERNET_TITLE, NETWORK_ERROR, SERVER_ERROR, NOTHING_FOUND,
        RESET, SEARCH, CLEAR,
    }

    enum class Tabs : AppStringKey { SPACE, CHAT, LAB, CACHE, SETTINGS }

    /** Texts of the empty screens of the skeleton; removed with the real screens. */
    enum class Placeholder : AppStringKey { SPACE, CHAT, LAB, CACHE, SETTINGS, COMING_SOON }
}
