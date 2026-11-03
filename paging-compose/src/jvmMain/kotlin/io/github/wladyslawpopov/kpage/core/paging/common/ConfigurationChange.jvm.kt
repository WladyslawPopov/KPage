package io.github.wladyslawpopov.kpage.core.paging.common

import androidx.compose.runtime.Composable

private val neverChanging: () -> Boolean = { false }

@Composable
internal actual fun rememberConfigurationChangeCheck(): () -> Boolean = neverChanging
