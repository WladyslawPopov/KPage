package io.github.wladyslawpopov.kpager.core.paging

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.wladyslawpopov.kpager.core.paging.common.VerticalScrollbar

@Composable
fun LazyGridWithScrollBars(
    containerModifier: Modifier = Modifier.fillMaxSize(),
    listModifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(8.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
    reverseLayout: Boolean = false,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    containerAlignment: Alignment = Alignment.TopCenter,
    columns: Int = 1,
    content: LazyGridScope.() -> Unit
) {
    Box(
        modifier = containerModifier,
        contentAlignment = containerAlignment
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = state,
            contentPadding = contentPadding,
            horizontalArrangement = horizontalArrangement,
            verticalArrangement = verticalArrangement,
            reverseLayout = reverseLayout,
            modifier = listModifier,
            flingBehavior = flingBehavior,
            content = content
        )

        VerticalScrollbar(
            state,
            Modifier
                .align(Alignment.CenterEnd),
            isReversed = reverseLayout
        )
    }
}
