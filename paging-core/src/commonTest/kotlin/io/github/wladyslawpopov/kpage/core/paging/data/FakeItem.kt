package io.github.wladyslawpopov.kpage.core.paging.data

import kotlinx.serialization.Serializable

@Serializable
data class FakeItem(
    val id: String,
    val title: String
)

/** A second item type with the same shape, to check that equal ids of different types do not collide. */
@Serializable
data class OtherItem(
    val id: String,
    val title: String
)
