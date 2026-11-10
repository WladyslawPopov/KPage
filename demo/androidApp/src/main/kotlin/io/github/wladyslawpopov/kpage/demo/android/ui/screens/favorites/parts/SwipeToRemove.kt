package io.github.wladyslawpopov.kpage.demo.android.ui.screens.favorites.parts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.wladyslawpopov.kpage.demo.android.theme.color
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.vector
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey

/**
 * A swipe to the start asks to remove the row and holds it open while the question is on screen
 * ([isAsking]). When the question is closed the row springs back; it leaves the list only when the
 * answer is yes and the list renews.
 */
@Composable
internal fun SwipeToRemove(
    enabled: Boolean,
    isAsking: Boolean,
    onRemove: () -> Unit,
    content: @Composable () -> Unit,
) {
    val currentOnRemove by rememberUpdatedState(onRemove)
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissBoxValue.EndToStart) currentOnRemove()
    }
    // Keyed by the question only: the value of the state changes while the row moves back, and an
    // effect keyed by it would cancel its own return.
    LaunchedEffect(isAsking) {
        if (!isAsking && state.currentValue != SwipeToDismissBoxValue.Settled) state.reset()
    }
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = enabled,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(AppColorKey.Status.FAIL.color()),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = AppIconKey.TRASH.vector(),
                    contentDescription = null,
                    tint = AppColorKey.Icon.ON_ACCENT.color(),
                    modifier = Modifier.padding(end = AppDimenKey.Padding.LARGE.dp()),
                )
            }
        },
    ) {
        // Opaque, so the background shows only where the row has moved away.
        Box(Modifier.background(AppColorKey.Container.BACKGROUND.color())) { content() }
    }
}
