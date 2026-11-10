package io.github.wladyslawpopov.kpage.demo.shared.strings

import androidx.compose.runtime.Composable
import io.github.wladyslawpopov.kpage.demo.shared.resources.Res
import io.github.wladyslawpopov.kpage.demo.shared.resources.chat_actions
import io.github.wladyslawpopov.kpage.demo.shared.resources.chat_copy
import io.github.wladyslawpopov.kpage.demo.shared.resources.chat_delete_chat
import io.github.wladyslawpopov.kpage.demo.shared.resources.chat_delete_chat_message
import io.github.wladyslawpopov.kpage.demo.shared.resources.chat_delete_chat_title
import io.github.wladyslawpopov.kpage.demo.shared.resources.chat_create
import io.github.wladyslawpopov.kpage.demo.shared.resources.chat_create_title
import io.github.wladyslawpopov.kpage.demo.shared.resources.chat_delete
import io.github.wladyslawpopov.kpage.demo.shared.resources.chat_delete_message
import io.github.wladyslawpopov.kpage.demo.shared.resources.chat_delete_title
import io.github.wladyslawpopov.kpage.demo.shared.resources.chat_empty
import io.github.wladyslawpopov.kpage.demo.shared.resources.chat_interlocutor_hint
import io.github.wladyslawpopov.kpage.demo.shared.resources.chat_message_hint
import io.github.wladyslawpopov.kpage.demo.shared.resources.chat_not_found
import io.github.wladyslawpopov.kpage.demo.shared.resources.chat_retry
import io.github.wladyslawpopov.kpage.demo.shared.resources.chat_scenario_chatty
import io.github.wladyslawpopov.kpage.demo.shared.resources.chat_scenario_flaky
import io.github.wladyslawpopov.kpage.demo.shared.resources.chat_scenario_historian
import io.github.wladyslawpopov.kpage.demo.shared.resources.chat_selected
import io.github.wladyslawpopov.kpage.demo.shared.resources.chat_send_failed
import io.github.wladyslawpopov.kpage.demo.shared.resources.chat_title_hint
import io.github.wladyslawpopov.kpage.demo.shared.resources.chat_today
import io.github.wladyslawpopov.kpage.demo.shared.resources.core_back
import io.github.wladyslawpopov.kpage.demo.shared.resources.core_cancel
import io.github.wladyslawpopov.kpage.demo.shared.resources.core_clear
import io.github.wladyslawpopov.kpage.demo.shared.resources.core_close
import io.github.wladyslawpopov.kpage.demo.shared.resources.core_copied
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
import io.github.wladyslawpopov.kpage.demo.shared.resources.favorites_empty
import io.github.wladyslawpopov.kpage.demo.shared.resources.favorites_remove
import io.github.wladyslawpopov.kpage.demo.shared.resources.favorites_remove_message
import io.github.wladyslawpopov.kpage.demo.shared.resources.favorites_remove_title
import io.github.wladyslawpopov.kpage.demo.shared.resources.favorites_selected
import io.github.wladyslawpopov.kpage.demo.shared.resources.placeholder_chat
import io.github.wladyslawpopov.kpage.demo.shared.resources.placeholder_coming_soon
import io.github.wladyslawpopov.kpage.demo.shared.resources.placeholder_lab
import io.github.wladyslawpopov.kpage.demo.shared.resources.placeholder_settings
import io.github.wladyslawpopov.kpage.demo.shared.resources.placeholder_space
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_album
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_albums
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_any
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_author
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
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_date
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_details
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_file_size
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_format
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_from
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_image
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_keyword
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_keywords
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_location
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_nasa_id
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_photographer
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_resolution
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_search_hint
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_show
import io.github.wladyslawpopov.kpage.demo.shared.resources.space_to
import io.github.wladyslawpopov.kpage.demo.shared.resources.tabs_chat
import io.github.wladyslawpopov.kpage.demo.shared.resources.tabs_favorites
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
        AppStringKey.Core.COPIED -> Res.string.core_copied
    }

    is AppStringKey.Tabs -> when (this) {
        AppStringKey.Tabs.SPACE -> Res.string.tabs_space
        AppStringKey.Tabs.FAVORITES -> Res.string.tabs_favorites
        AppStringKey.Tabs.CHAT -> Res.string.tabs_chat
        AppStringKey.Tabs.LAB -> Res.string.tabs_lab
        AppStringKey.Tabs.SETTINGS -> Res.string.tabs_settings
    }

    is AppStringKey.Favorites -> when (this) {
        AppStringKey.Favorites.EMPTY -> Res.string.favorites_empty
        AppStringKey.Favorites.SELECTED -> Res.string.favorites_selected
        AppStringKey.Favorites.REMOVE -> Res.string.favorites_remove
        AppStringKey.Favorites.REMOVE_TITLE -> Res.string.favorites_remove_title
        AppStringKey.Favorites.REMOVE_MESSAGE -> Res.string.favorites_remove_message
    }

    is AppStringKey.Chat -> when (this) {
        AppStringKey.Chat.EMPTY -> Res.string.chat_empty
        AppStringKey.Chat.CREATE -> Res.string.chat_create
        AppStringKey.Chat.CREATE_TITLE -> Res.string.chat_create_title
        AppStringKey.Chat.TITLE_HINT -> Res.string.chat_title_hint
        AppStringKey.Chat.INTERLOCUTOR_HINT -> Res.string.chat_interlocutor_hint
        AppStringKey.Chat.SELECTED -> Res.string.chat_selected
        AppStringKey.Chat.DELETE -> Res.string.chat_delete
        AppStringKey.Chat.DELETE_TITLE -> Res.string.chat_delete_title
        AppStringKey.Chat.DELETE_MESSAGE -> Res.string.chat_delete_message
        AppStringKey.Chat.DELETE_CHAT -> Res.string.chat_delete_chat
        AppStringKey.Chat.DELETE_CHAT_TITLE -> Res.string.chat_delete_chat_title
        AppStringKey.Chat.DELETE_CHAT_MESSAGE -> Res.string.chat_delete_chat_message
        AppStringKey.Chat.ACTIONS -> Res.string.chat_actions
        AppStringKey.Chat.MESSAGE_HINT -> Res.string.chat_message_hint
        AppStringKey.Chat.SEND_FAILED -> Res.string.chat_send_failed
        AppStringKey.Chat.RETRY -> Res.string.chat_retry
        AppStringKey.Chat.COPY -> Res.string.chat_copy
        AppStringKey.Chat.TODAY -> Res.string.chat_today
        AppStringKey.Chat.NOT_FOUND -> Res.string.chat_not_found
        AppStringKey.Chat.SCENARIO_CHATTY -> Res.string.chat_scenario_chatty
        AppStringKey.Chat.SCENARIO_HISTORIAN -> Res.string.chat_scenario_historian
        AppStringKey.Chat.SCENARIO_FLAKY -> Res.string.chat_scenario_flaky
    }

    is AppStringKey.Space -> when (this) {
        AppStringKey.Space.SEARCH_HINT -> Res.string.space_search_hint
        AppStringKey.Space.CENTER -> Res.string.space_center
        AppStringKey.Space.FROM -> Res.string.space_from
        AppStringKey.Space.TO -> Res.string.space_to
        AppStringKey.Space.ANY -> Res.string.space_any
        AppStringKey.Space.SHOW -> Res.string.space_show
        AppStringKey.Space.DETAILS -> Res.string.space_details
        AppStringKey.Space.KEYWORD -> Res.string.space_keyword
        AppStringKey.Space.KEYWORDS -> Res.string.space_keywords
        AppStringKey.Space.ALBUM -> Res.string.space_album
        AppStringKey.Space.ALBUMS -> Res.string.space_albums
        AppStringKey.Space.IMAGE -> Res.string.space_image
        AppStringKey.Space.AUTHOR -> Res.string.space_author
        AppStringKey.Space.PHOTOGRAPHER -> Res.string.space_photographer
        AppStringKey.Space.LOCATION -> Res.string.space_location
        AppStringKey.Space.DATE -> Res.string.space_date
        AppStringKey.Space.FORMAT -> Res.string.space_format
        AppStringKey.Space.RESOLUTION -> Res.string.space_resolution
        AppStringKey.Space.FILE_SIZE -> Res.string.space_file_size
        AppStringKey.Space.NASA_ID -> Res.string.space_nasa_id
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
        AppStringKey.Placeholder.SETTINGS -> Res.string.placeholder_settings
        AppStringKey.Placeholder.COMING_SOON -> Res.string.placeholder_coming_soon
    }
}

/** The text of a key for Compose UIs (Android, Desktop). */
@Composable
fun AppStringKey.asString(): String = stringResource(resource())
