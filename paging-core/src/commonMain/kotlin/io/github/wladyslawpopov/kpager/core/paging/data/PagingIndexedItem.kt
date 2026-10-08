package io.github.wladyslawpopov.kpager.core.paging.data

/**
 * Implement for UI lists containing extra rows such as headers or separators.
 * Server items and their placeholders keep their original global [serverIndex];
 * decorative rows return null. Ordinary lists do not need this interface.
 */
interface PagingIndexedItem {
    val serverIndex: Int?
}
