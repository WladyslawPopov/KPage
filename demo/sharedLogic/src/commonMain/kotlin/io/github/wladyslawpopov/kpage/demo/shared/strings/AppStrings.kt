package io.github.wladyslawpopov.kpage.demo.shared.strings

import androidx.compose.runtime.Composable
import io.github.wladyslawpopov.kpage.demo.shared.resources.Res
import io.github.wladyslawpopov.kpage.demo.shared.resources.core_back
import io.github.wladyslawpopov.kpage.demo.shared.resources.core_cancel
import io.github.wladyslawpopov.kpage.demo.shared.resources.core_clear
import io.github.wladyslawpopov.kpage.demo.shared.resources.core_close
import io.github.wladyslawpopov.kpage.demo.shared.resources.core_done
import io.github.wladyslawpopov.kpage.demo.shared.resources.core_filters
import io.github.wladyslawpopov.kpage.demo.shared.resources.core_network_error
import io.github.wladyslawpopov.kpage.demo.shared.resources.core_no_internet_title
import io.github.wladyslawpopov.kpage.demo.shared.resources.core_nothing_found
import io.github.wladyslawpopov.kpage.demo.shared.resources.core_ok
import io.github.wladyslawpopov.kpage.demo.shared.resources.core_oops_title
import io.github.wladyslawpopov.kpage.demo.shared.resources.core_refresh
import io.github.wladyslawpopov.kpage.demo.shared.resources.core_reset
import io.github.wladyslawpopov.kpage.demo.shared.resources.core_retry
import io.github.wladyslawpopov.kpage.demo.shared.resources.core_search
import io.github.wladyslawpopov.kpage.demo.shared.resources.core_server_error
import io.github.wladyslawpopov.kpage.demo.shared.resources.placeholder_cache
import io.github.wladyslawpopov.kpage.demo.shared.resources.placeholder_chat
import io.github.wladyslawpopov.kpage.demo.shared.resources.placeholder_coming_soon
import io.github.wladyslawpopov.kpage.demo.shared.resources.placeholder_lab
import io.github.wladyslawpopov.kpage.demo.shared.resources.placeholder_settings
import io.github.wladyslawpopov.kpage.demo.shared.resources.placeholder_space
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_any
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_center
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_center_afrc
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_center_arc
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_center_grc
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_center_gsfc
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_center_hq
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_center_jpl
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_center_jsc
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_center_ksc
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_center_larc
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_center_lrc
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_center_msfc
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_center_ssc
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_from
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_search_hint
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_show
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_to
import io.github.wladyslawpopov.kpage.demo.shared.resources.tabs_cache
import io.github.wladyslawpopov.kpage.demo.shared.resources.tabs_chat
import io.github.wladyslawpopov.kpage.demo.shared.resources.tabs_lab
import io.github.wladyslawpopov.kpage.demo.shared.resources.tabs_settings
import io.github.wladyslawpopov.kpage.demo.shared.resources.tabs_space
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Every interface string; a key without a string does not compile. */
fun AppStringKey.resource(): StringResource = when (this) {
    is AppStringKey.Core -> when (this) {
        AppStringKey.Core.OK -> Res.string.core_ok
        AppStringKey.Core.CANCEL -> Res.string.core_cancel
        AppStringKey.Core.RETRY -> Res.string.core_retry
        AppStringKey.Core.REFRESH -> Res.string.core_refresh
        AppStringKey.Core.CLOSE -> Res.string.core_close
        AppStringKey.Core.BACK -> Res.string.core_back
        AppStringKey.Core.OOPS_TITLE -> Res.string.core_oops_title
        AppStringKey.Core.NO_INTERNET_TITLE -> Res.string.core_no_internet_title
        AppStringKey.Core.NETWORK_ERROR -> Res.string.core_network_error
        AppStringKey.Core.SERVER_ERROR -> Res.string.core_server_error
        AppStringKey.Core.NOTHING_FOUND -> Res.string.core_nothing_found
        AppStringKey.Core.RESET -> Res.string.core_reset
        AppStringKey.Core.SEARCH -> Res.string.core_search
        AppStringKey.Core.CLEAR -> Res.string.core_clear
        AppStringKey.Core.FILTERS -> Res.string.core_filters
        AppStringKey.Core.DONE -> Res.string.core_done
    }

    is AppStringKey.Tabs -> when (this) {
        AppStringKey.Tabs.SPACE -> Res.string.tabs_space
        AppStringKey.Tabs.CHAT -> Res.string.tabs_chat
        AppStringKey.Tabs.LAB -> Res.string.tabs_lab
        AppStringKey.Tabs.CACHE -> Res.string.tabs_cache
        AppStringKey.Tabs.SETTINGS -> Res.string.tabs_settings
    }

    is AppStringKey.Space -> when (this) {
        AppStringKey.Space.SEARCH_HINT -> Res.string.space_search_hint
        AppStringKey.Space.CENTER -> Res.string.space_center
        AppStringKey.Space.FROM -> Res.string.space_from
        AppStringKey.Space.TO -> Res.string.space_to
        AppStringKey.Space.ANY -> Res.string.space_any
        AppStringKey.Space.SHOW -> Res.string.space_show
        AppStringKey.Space.CENTER_AFRC -> Res.string.space_center_afrc
        AppStringKey.Space.CENTER_ARC -> Res.string.space_center_arc
        AppStringKey.Space.CENTER_GRC -> Res.string.space_center_grc
        AppStringKey.Space.CENTER_GSFC -> Res.string.space_center_gsfc
        AppStringKey.Space.CENTER_HQ -> Res.string.space_center_hq
        AppStringKey.Space.CENTER_JPL -> Res.string.space_center_jpl
        AppStringKey.Space.CENTER_JSC -> Res.string.space_center_jsc
        AppStringKey.Space.CENTER_KSC -> Res.string.space_center_ksc
        AppStringKey.Space.CENTER_LARC -> Res.string.space_center_larc
        AppStringKey.Space.CENTER_LRC -> Res.string.space_center_lrc
        AppStringKey.Space.CENTER_MSFC -> Res.string.space_center_msfc
        AppStringKey.Space.CENTER_SSC -> Res.string.space_center_ssc
    }

    is AppStringKey.Placeholder -> when (this) {
        AppStringKey.Placeholder.SPACE -> Res.string.placeholder_space
        AppStringKey.Placeholder.CHAT -> Res.string.placeholder_chat
        AppStringKey.Placeholder.LAB -> Res.string.placeholder_lab
        AppStringKey.Placeholder.CACHE -> Res.string.placeholder_cache
        AppStringKey.Placeholder.SETTINGS -> Res.string.placeholder_settings
        AppStringKey.Placeholder.COMING_SOON -> Res.string.placeholder_coming_soon
    }
}

/** The text of a key for Compose UIs (Android, Desktop). */
@Composable
fun AppStringKey.asString(): String = stringResource(resource())
