package io.github.wladyslawpopov.kpage.demo.domain.space

/** What the Space list shows: a search text and filters. A new query is a new list. */
data class SpaceQuery(
    val text: String = "",
    val center: SpaceCenter? = null,
    val yearStart: Int? = null,
    val yearEnd: Int? = null,
)

/** NASA centers the list can be filtered by. */
enum class SpaceCenter { AFRC, ARC, GRC, GSFC, HQ, JPL, JSC, KSC, LARC, LRC, MSFC, SSC }
