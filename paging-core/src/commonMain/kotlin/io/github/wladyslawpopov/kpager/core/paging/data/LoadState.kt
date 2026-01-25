package io.github.wladyslawpopov.kpager.core.paging.data

import kotlinx.serialization.Serializable
@Serializable
sealed class LoadState {
    @Serializable
    data object IDLE : LoadState()

    @Serializable
    data object NEXT : LoadState()

    @Serializable
    data object INITIAL : LoadState()

    @Serializable
    data object END : LoadState()

    @Serializable
    data class ERROR(val error: String) : LoadState()
}
