package io.github.wladyslawpopov.kpage.demo.android.ui.screens.favorites.parts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.wladyslawpopov.kpage.demo.android.theme.asString
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.style
import io.github.wladyslawpopov.kpage.demo.android.theme.vector
import io.github.wladyslawpopov.kpage.demo.android.ui.barItemColor
import io.github.wladyslawpopov.kpage.demo.android.ui.widgets.BarIconButton
import io.github.wladyslawpopov.kpage.demo.presentation.screens.favorites.FavoritesIntent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.favorites.FavoritesState
import io.github.wladyslawpopov.kpage.demo.shared.strings.asString
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppTypographyKey

/**
 * The bar of the Favorites tab: the title, or in the selection mode the count with a cross that leaves
 * the mode and the actions on the chosen images (for now, the removal).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesTopBar(state: FavoritesState, onIntent: (FavoritesIntent) -> Unit) {
    val isSelecting = state.isSelecting
    TopAppBar(
        title = {
            Text(
                text = if (isSelecting) state.selectionTitle.asString() else state.topBar.title.asString(),
                style = AppTypographyKey.TITLE.style(),
                modifier = Modifier
                    .background(barItemColor(), CircleShape)
                    .padding(horizontal = AppDimenKey.Padding.LARGE.dp(), vertical = AppDimenKey.Padding.SMALL.dp()),
            )
        },
        navigationIcon = {
            if (isSelecting) {
                BarIconButton(
                    onClick = { onIntent(FavoritesIntent.ClearSelection) },
                    modifier = Modifier.padding(start = AppDimenKey.Padding.SMALL.dp()),
                ) {
                    Icon(AppIconKey.CLOSE.vector(), contentDescription = AppStringKey.Core.CLOSE.asString())
                }
            }
        },
        actions = {
            if (isSelecting) {
                BarIconButton(onClick = { onIntent(FavoritesIntent.DeleteSelected) }) {
                    Icon(AppIconKey.TRASH.vector(), contentDescription = AppStringKey.Favorites.REMOVE.asString())
                }
            }
            // With the 4 dp of the bar and the 4 dp of the button, the edge gap matches the 16 dp at the start.
            Spacer(Modifier.width(AppDimenKey.Padding.SMALL.dp()))
        },
        windowInsets = WindowInsets(0),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
    )
}
