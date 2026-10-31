package io.github.wladyslawpopov.kpage.core.paging.data.state

sealed class LoadState {
    /** The window is loaded and there is more data after it. */
    data object IDLE : LoadState()

    /** Pages of the window are being loaded while scrolling. */
    data object NEXT : LoadState()

    /** Loading with nothing to show: there is no cache for this list. */
    data object INITIAL : LoadState()

    /** A refresh is running on top of the shown cache. */
    data object REFRESH : LoadState()

    /** The window is loaded and reaches the end of the list. */
    data object END : LoadState()

    /** The last load failed; the cache is intact. */
    data class ERROR(val error: Throwable) : LoadState()
}
