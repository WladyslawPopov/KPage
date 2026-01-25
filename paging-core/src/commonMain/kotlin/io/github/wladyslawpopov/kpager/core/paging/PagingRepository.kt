package io.github.wladyslawpopov.kpager.core.paging

import kotlinx.serialization.KSerializer
import io.github.wladyslawpopov.kpager.core.paging.data.PaginatorConfig
import io.github.wladyslawpopov.kpager.core.paging.data.PagingResult
import io.github.wladyslawpopov.kpager.core.paging.data.PagerPayload

class PagingRepository() {
    fun <T : Any> getPaginatedItems(
        queryKey: String,
        getPage: suspend (Int) -> PagerPayload<T>,
        serializer: KSerializer<T>,
        config: PaginatorConfig = PaginatorConfig(),
        idExtractor: (T) -> Long
    ): PagingResult<T> {

        val paginator = StablePaginator(
            serializer = serializer,
            queryKey = queryKey,
            getPage = getPage,
            config = config,
            idExtractor = idExtractor
        )

        return PagingResult(
            itemsMap = paginator.itemsMap,
            loadState = paginator.loadState,
            totalCount = paginator.totalCount,
            paginator = paginator
        )
    }
}
