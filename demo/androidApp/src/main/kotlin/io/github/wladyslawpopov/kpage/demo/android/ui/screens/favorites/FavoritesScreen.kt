package io.github.wladyslawpopov.kpage.demo.android.ui.screens.favorites

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.wladyslawpopov.kpage.demo.android.ui.page.PageLayout
import io.github.wladyslawpopov.kpage.demo.android.ui.paging.PagedGrid
import io.github.wladyslawpopov.kpage.demo.android.ui.paging.rememberCanHideBars
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.favorites.parts.FavoritesTopBar
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.favorites.parts.RemoveDialog
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.favorites.parts.SwipeToRemove
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.space.listing.cards.ImageRow
import io.github.wladyslawpopov.kpage.demo.presentation.screens.favorites.FavoritesComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.favorites.FavoritesIntent
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey

@Composable
fun FavoritesScreen(component: FavoritesComponent) {
    val state by component.state.collectAsState()
    val gridState = rememberLazyGridState()
    val canHideBars by rememberCanHideBars(gridState)

    // Back leaves the selection mode first, as it closes an open search.
    BackHandler(enabled = state.isSelecting) { component.onIntent(FavoritesIntent.ClearSelection) }
    if (state.pendingDelete.isNotEmpty()) {
        RemoveDialog(
            onConfirm = { component.onIntent(FavoritesIntent.ConfirmDelete) },
            onCancel = { component.onIntent(FavoritesIntent.CancelDelete) },
        )
    }
    PageLayout(
        page = state.page,
        onPageIntent = component::onPageIntent,
        hideBottomBarOnScroll = canHideBars,
        header = { FavoritesTopBar(state = state, onIntent = component::onIntent) },
    ) { padding ->
        PagedGrid(
            list = state.list,
            keyOf = { it.id },
            gridState = gridState,
            contentPadding = padding,
            // Rows keep their own padding and a divider.
            spacing = AppDimenKey.Padding.NONE,
            placeholderContent = { ImageRow(null, onClick = {}) },
            itemContent = { image, _ ->
                SwipeToRemove(
                    enabled = !state.isSelecting,
                    isAsking = state.pendingDelete.isNotEmpty(),
                    onRemove = { component.onIntent(FavoritesIntent.DeleteOne(image.id)) },
                ) {
                    ImageRow(
                        image = image,
                        onClick = { component.onIntent(FavoritesIntent.Click(image.id)) },
                        onLongClick = { component.onIntent(FavoritesIntent.LongClick(image.id)) },
                    ) {
                        // The row itself takes the tap, so the box only shows the choice.
                        if (state.isSelecting) Checkbox(checked = image.id in state.selected, onCheckedChange = null)
                    }
                }
            },
        )
    }
}
