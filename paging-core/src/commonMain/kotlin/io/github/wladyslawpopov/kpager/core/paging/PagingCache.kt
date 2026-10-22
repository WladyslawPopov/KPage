package io.github.wladyslawpopov.kpager.core.paging

import io.github.wladyslawpopov.kpager.cache.PagingDataBase
import io.github.wladyslawpopov.kpager.core.paging.common.dbDispatcher
import kotlinx.coroutines.withContext

/** Clears persisted pages after their owning paginators have been closed. */
suspend fun PagingDataBase.clearCache() {
    withContext(dbDispatcher()) {
        transaction {
            listingEntryQueries.clearAll()
            pageMetadataQueries.clearAll()
            itemRawQueries.clearAll()
        }
    }
}
