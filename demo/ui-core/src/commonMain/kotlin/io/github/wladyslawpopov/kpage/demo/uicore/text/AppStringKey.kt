package io.github.wladyslawpopov.kpage.demo.uicore.text

/** Interface strings; the app layer turns them into text. */
sealed interface AppStringKey {
    enum class Core : AppStringKey {
        OK, CANCEL, RETRY, REFRESH, CLOSE, BACK,
        OOPS_TITLE, NO_INTERNET_TITLE, NETWORK_ERROR, SERVER_ERROR, NOTHING_FOUND,
        RESET, SEARCH, CLEAR, FILTERS, DONE, COPIED,
    }

    enum class Tabs : AppStringKey { SPACE, CHAT, LAB, CACHE, SETTINGS }

    /** Texts of the Space screens: the search hint, the filters, the details; one name per NASA center. */
    enum class Space : AppStringKey {
        SEARCH_HINT, CENTER, FROM, TO, ANY, SHOW,
        DETAILS, KEYWORD, KEYWORDS, ALBUM, ALBUMS, IMAGE, AUTHOR, PHOTOGRAPHER, LOCATION, DATE,
        FORMAT, RESOLUTION, FILE_SIZE, NASA_ID,
        CENTER_AFRC, CENTER_ARC, CENTER_GRC, CENTER_GSFC, CENTER_HQ, CENTER_JPL,
        CENTER_JSC, CENTER_KSC, CENTER_LARC, CENTER_LRC, CENTER_MSFC, CENTER_SSC,
    }

    /** Texts of the empty screens of the skeleton; removed with the real screens. */
    enum class Placeholder : AppStringKey { SPACE, CHAT, LAB, CACHE, SETTINGS, COMING_SOON }
}
