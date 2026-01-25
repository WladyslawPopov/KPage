package io.github.wladyslawpopov.kpager.core.paging.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun VerticalScrollbar(scrollState: Any, modifier: Modifier, isReversed: Boolean = false)

@Composable
expect fun HorizontalScrollbar(scrollState: Any, modifier: Modifier, isReversed: Boolean = false)
