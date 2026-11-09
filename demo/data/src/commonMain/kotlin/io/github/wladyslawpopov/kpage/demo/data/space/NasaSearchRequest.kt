package io.github.wladyslawpopov.kpage.demo.data.space

/** One page of a NASA search: the filters plus the page asked for. [page] starts at 1. */
internal data class NasaSearchRequest(
    val text: String? = null,
    val center: String? = null,
    val yearStart: Int? = null,
    val yearEnd: Int? = null,
    val keywords: String? = null,
    val photographer: String? = null,
    val secondaryCreator: String? = null,
    val location: String? = null,
    val album: String? = null,
    val page: Int,
    val pageSize: Int,
)
