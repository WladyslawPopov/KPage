package io.github.wladyslawpopov.kpage.demo.domain.space

import io.github.wladyslawpopov.kpage.core.paging.data.paginator.Paginator
import kotlinx.coroutines.CoroutineScope

/** Data of the Space tab. */
interface SpaceRepository {
    /** The paginator of the list for [query]; it lives as long as [scope]. */
    fun images(query: SpaceQuery, scope: CoroutineScope): Paginator<SpaceImage>
}
