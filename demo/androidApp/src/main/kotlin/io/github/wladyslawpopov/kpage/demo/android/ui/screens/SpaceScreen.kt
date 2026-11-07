package io.github.wladyslawpopov.kpage.demo.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.color
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.style
import io.github.wladyslawpopov.kpage.demo.android.theme.vector
import io.github.wladyslawpopov.kpage.demo.android.ui.page.PageLayout
import io.github.wladyslawpopov.kpage.demo.android.ui.paging.PagedGrid
import io.github.wladyslawpopov.kpage.demo.android.ui.widgets.RemoteImage
import io.github.wladyslawpopov.kpage.demo.android.ui.widgets.TopBar
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceImage
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.SpaceComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.SpaceIntent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.SpaceLayout
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppTypographyKey

@Composable
fun SpaceScreen(component: SpaceComponent) {
    val state by component.state.collectAsState()
    val isGrid = state.layout == SpaceLayout.GRID
    val onFavorite = { image: SpaceImage ->
        component.onIntent(SpaceIntent.SetFavorite(image.id, !image.isFavorite))
    }
    PageLayout(
        page = state.page,
        onPageIntent = component::onPageIntent,
        // A tab root: the back button is never shown.
        header = {
            TopBar(state.topBar, onBack = {}) {
                // Shows the layout a tap switches to.
                IconButton(onClick = { component.onIntent(SpaceIntent.ToggleLayout) }) {
                    Icon((if (isGrid) AppIconKey.LAYOUT_LIST else AppIconKey.LAYOUT_GRID).vector(), contentDescription = null)
                }
            }
        },
    ) { padding ->
        PagedGrid(
            list = state.list,
            keyOf = { it.id },
            columns = if (isGrid) GRID_COLUMNS else 1,
            contentPadding = padding,
            modifier = Modifier.padding(horizontal = AppDimenKey.Padding.SMALL.dp()),
            placeholderContent = { if (isGrid) ImageTile(null) {} else ImageRow(null) {} },
            itemContent = { image, _ ->
                if (isGrid) ImageTile(image) { onFavorite(image) } else ImageRow(image) { onFavorite(image) }
            },
        )
    }
}

/** A list card; a null [image] is the placeholder of the same size. */
@Composable
private fun ImageRow(image: SpaceImage?, onFavorite: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimenKey.Padding.MEDIUM.dp()),
        modifier = Modifier.fillMaxWidth().padding(start = AppDimenKey.Padding.SMALL.dp()),
    ) {
        val thumbnail = Modifier.size(THUMBNAIL_SIZE).clip(RoundedCornerShape(AppDimenKey.CornerRadius.MEDIUM.dp()))
        if (image == null) {
            Box(thumbnail.background(AppColorKey.Container.SECONDARY.color()))
        } else {
            RemoteImage(url = image.previewUrl, contentDescription = null, modifier = thumbnail)
        }
        Column(Modifier.weight(1f)) {
            ImageTexts(image)
        }
        // Keeps the height and the text width of a placeholder equal to a card.
        Box(Modifier.size(HEART_SIZE), contentAlignment = Alignment.Center) {
            if (image != null) Heart(image.isFavorite, onFavorite)
        }
    }
}

/** A grid card; a null [image] is the placeholder of the same size. */
@Composable
private fun ImageTile(image: SpaceImage?, onFavorite: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(AppDimenKey.CornerRadius.MEDIUM.dp()))
                .background(AppColorKey.Container.SECONDARY.color()),
        ) {
            if (image != null) {
                RemoteImage(url = image.previewUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
                Heart(
                    isFavorite = image.isFavorite,
                    onClick = onFavorite,
                    onImage = true,
                    modifier = Modifier.align(Alignment.TopEnd).padding(AppDimenKey.Padding.EXTRA_SMALL.dp()),
                )
            }
        }
        Column(Modifier.padding(vertical = AppDimenKey.Padding.EXTRA_SMALL.dp())) {
            ImageTexts(image, details = image?.date)
        }
    }
}

/** The title in two lines and the details under it; grey bars for a placeholder. */
@Composable
private fun ImageTexts(
    image: SpaceImage?,
    details: String? = image?.let { listOfNotNull(it.date, it.center).joinToString(" · ") },
) {
    if (image == null) {
        TextBar(AppTypographyKey.BODY, lines = 2, widthFraction = 0.9f)
        TextBar(AppTypographyKey.CAPTION, lines = 1, widthFraction = 0.5f)
        return
    }
    Text(
        text = image.title,
        style = AppTypographyKey.BODY.style(),
        color = AppColorKey.Text.PRIMARY.color(),
        maxLines = 2,
        minLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    Text(
        text = details.orEmpty(),
        style = AppTypographyKey.CAPTION.style(),
        color = AppColorKey.Text.SECONDARY.color(),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** A grey bar as tall as [lines] of text in [typography], so a placeholder keeps the card height. */
@Composable
private fun TextBar(typography: AppTypographyKey, lines: Int, widthFraction: Float) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Text(text = "", style = typography.style(), minLines = lines)
        Box(
            Modifier
                .fillMaxWidth(widthFraction)
                .height(AppDimenKey.IconSize.EXTRA_SMALL.dp())
                .background(AppColorKey.Container.SECONDARY.color(), RoundedCornerShape(AppDimenKey.CornerRadius.SMALL.dp())),
        )
    }
}

/** The favorite mark; over an image it gets a background to stay visible on any picture. */
@Composable
private fun Heart(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onImage: Boolean = false,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        colors = if (onImage) {
            IconButtonDefaults.iconButtonColors(containerColor = AppColorKey.Container.ELEVATED.color().copy(alpha = 0.8f))
        } else {
            IconButtonDefaults.iconButtonColors()
        },
    ) {
        Icon(
            imageVector = (if (isFavorite) AppIconKey.FAVORITE else AppIconKey.FAVORITE_OFF).vector(),
            contentDescription = null,
            tint = if (isFavorite) AppColorKey.Icon.ACCENT.color() else AppColorKey.Icon.SECONDARY.color(),
        )
    }
}

private const val GRID_COLUMNS = 2
private val THUMBNAIL_SIZE = 72.dp
private val HEART_SIZE = 48.dp
