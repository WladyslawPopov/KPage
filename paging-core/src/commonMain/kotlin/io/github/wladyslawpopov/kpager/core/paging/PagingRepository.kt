package io.github.wladyslawpopov.kpager.core.paging

import kotlinx.serialization.KSerializer
import io.github.wladyslawpopov.kpager.core.paging.data.PaginatorConfig
import io.github.wladyslawpopov.kpager.core.paging.data.PagerPayload
import io.github.wladyslawpopov.kpager.core.paging.data.Paginator

class PagingRepository {
    fun <T : Any> getPaginatedItems(
        queryKey: String,
        getPage: suspend (Int) -> PagerPayload<T>,
        serializer: KSerializer<T>,
        config: PaginatorConfig = PaginatorConfig(),
        idExtractor: (T) -> String
    ): Paginator<T> {

        return StablePaginator(
            serializer = serializer,
            queryKey = queryKey,
            getPage = getPage,
            config = config,
            idExtractor = idExtractor
        )
    }
}
