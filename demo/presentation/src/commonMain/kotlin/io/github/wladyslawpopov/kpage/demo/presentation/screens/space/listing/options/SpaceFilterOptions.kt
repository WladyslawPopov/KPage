package io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.options

import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceCenter
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceTagKind
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.SpaceIntent
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey
import kotlin.time.Clock

/** What the filters of the Space screen offer; the UI draws the choices and sends [SpaceIntent.SetFilters]. */
object SpaceFilterOptions {
    val centers: List<SpaceCenter> = SpaceCenter.entries

    /** NASA has images from the 1900s. */
    val firstYear: Int = 1900

    /** The current year, so the years the filter offers do not run out. */
    val lastYear: Int get() = Clock.System.now().toString().take(4).toInt()

    fun title(center: SpaceCenter): AppStringKey = when (center) {
        SpaceCenter.AFRC -> AppStringKey.Space.CENTER_AFRC
        SpaceCenter.ARC -> AppStringKey.Space.CENTER_ARC
        SpaceCenter.GRC -> AppStringKey.Space.CENTER_GRC
        SpaceCenter.GSFC -> AppStringKey.Space.CENTER_GSFC
        SpaceCenter.HQ -> AppStringKey.Space.CENTER_HQ
        SpaceCenter.JPL -> AppStringKey.Space.CENTER_JPL
        SpaceCenter.JSC -> AppStringKey.Space.CENTER_JSC
        SpaceCenter.KSC -> AppStringKey.Space.CENTER_KSC
        SpaceCenter.LARC -> AppStringKey.Space.CENTER_LARC
        SpaceCenter.LRC -> AppStringKey.Space.CENTER_LRC
        SpaceCenter.MSFC -> AppStringKey.Space.CENTER_MSFC
        SpaceCenter.SSC -> AppStringKey.Space.CENTER_SSC
    }

    fun title(kind: SpaceTagKind): AppStringKey = when (kind) {
        SpaceTagKind.KEYWORD -> AppStringKey.Space.KEYWORD
        SpaceTagKind.PHOTOGRAPHER -> AppStringKey.Space.PHOTOGRAPHER
        SpaceTagKind.CREATOR -> AppStringKey.Space.AUTHOR
        SpaceTagKind.LOCATION -> AppStringKey.Space.LOCATION
        SpaceTagKind.ALBUM -> AppStringKey.Space.ALBUM
    }

    fun icon(kind: SpaceTagKind): AppIconKey = when (kind) {
        SpaceTagKind.KEYWORD -> AppIconKey.KEYWORD
        SpaceTagKind.PHOTOGRAPHER -> AppIconKey.PHOTOGRAPHER
        SpaceTagKind.CREATOR -> AppIconKey.AUTHOR
        SpaceTagKind.LOCATION -> AppIconKey.LOCATION
        SpaceTagKind.ALBUM -> AppIconKey.ALBUM
    }
}
