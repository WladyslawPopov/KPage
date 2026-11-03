package io.github.wladyslawpopov.kpage.demo.shared.strings

import androidx.compose.runtime.Composable
import io.github.wladyslawpopov.kpage.demo.shared.resources.Res
import io.github.wladyslawpopov.kpage.demo.shared.resources.core_back
import io.github.wladyslawpopov.kpage.demo.shared.resources.core_cancel
import io.github.wladyslawpopov.kpage.demo.shared.resources.core_close
import io.github.wladyslawpopov.kpage.demo.shared.resources.core_network_error
import io.github.wladyslawpopov.kpage.demo.shared.resources.core_no_internet_title
import io.github.wladyslawpopov.kpage.demo.shared.resources.core_nothing_found
import io.github.wladyslawpopov.kpage.demo.shared.resources.core_ok
import io.github.wladyslawpopov.kpage.demo.shared.resources.core_oops_title
import io.github.wladyslawpopov.kpage.demo.shared.resources.core_refresh
import io.github.wladyslawpopov.kpage.demo.shared.resources.core_retry
import io.github.wladyslawpopov.kpage.demo.shared.resources.core_server_error
import io.github.wladyslawpopov.kpage.demo.shared.resources.placeholder_cache
import io.github.wladyslawpopov.kpage.demo.shared.resources.placeholder_chat
import io.github.wladyslawpopov.kpage.demo.shared.resources.placeholder_coming_soon
import io.github.wladyslawpopov.kpage.demo.shared.resources.placeholder_lab
import io.github.wladyslawpopov.kpage.demo.shared.resources.placeholder_settings
import io.github.wladyslawpopov.kpage.demo.shared.resources.placeholder_space
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
    }

    is AppStringKey.Tabs -> when (this) {
        AppStringKey.Tabs.SPACE -> Res.string.tabs_space
        AppStringKey.Tabs.CHAT -> Res.string.tabs_chat
        AppStringKey.Tabs.LAB -> Res.string.tabs_lab
        AppStringKey.Tabs.CACHE -> Res.string.tabs_cache
        AppStringKey.Tabs.SETTINGS -> Res.string.tabs_settings
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
