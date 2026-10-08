package io.github.wladyslawpopov.kpager.core.paging.data

sealed class LoadState {
    data object IDLE : LoadState()

    data object NEXT : LoadState()

    data object INITIAL : LoadState()

    data object END : LoadState()

    data class ERROR(val error: Throwable) : LoadState()
}
