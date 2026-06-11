package io.github.wladyslawpopov.kpager.core.paging.data

import kotlinx.serialization.Serializable

@Serializable
data class FakeItem(
    val id: String,
    val title: String
)
