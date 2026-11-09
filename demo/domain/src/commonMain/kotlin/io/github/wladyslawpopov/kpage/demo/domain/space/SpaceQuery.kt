package io.github.wladyslawpopov.kpage.demo.domain.space

import kotlinx.serialization.Serializable

/**
 * What the Space list shows: a search text and filters. A new query is a new list. Serializable,
 * because a list opened with a filter keeps its query in the route.
 *
 * @property tag One more exact filter, for example the photographer of an image the user came from.
 * NASA's text search is no replacement for it: it splits the value into words, finds far more, and
 * finds nothing for a value with brackets or commas.
 */
@Serializable
data class SpaceQuery(
    val text: String = "",
    val center: SpaceCenter? = null,
    val yearStart: Int? = null,
    val yearEnd: Int? = null,
    val tag: SpaceTag? = null,
)

/** NASA centers the list can be filtered by. */
enum class SpaceCenter { AFRC, ARC, GRC, GSFC, HQ, JPL, JSC, KSC, LARC, LRC, MSFC, SSC }

/** A field of an image the list can be filtered by, with its value. */
@Serializable
data class SpaceTag(val kind: SpaceTagKind, val value: String)

enum class SpaceTagKind { KEYWORD, PHOTOGRAPHER, CREATOR, LOCATION, ALBUM }
