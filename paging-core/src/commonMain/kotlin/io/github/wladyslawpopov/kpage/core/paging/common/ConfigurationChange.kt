package io.github.wladyslawpopov.kpage.core.paging.common

import androidx.compose.runtime.Composable

/**
 * Returns a check that is true while the host is being recreated for a configuration change.
 * Such a recreation is not leaving the list. Only Android recreates the composition this way.
 */
@Composable
internal expect fun rememberConfigurationChangeCheck(): () -> Boolean
