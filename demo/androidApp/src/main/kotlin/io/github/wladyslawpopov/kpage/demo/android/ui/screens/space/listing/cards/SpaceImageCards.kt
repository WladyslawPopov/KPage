package io.github.wladyslawpopov.kpage.demo.android.ui.screens.space.listing.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.color
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.style
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.space.common.Heart
import io.github.wladyslawpopov.kpage.demo.android.ui.widgets.RemoteImage
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceImage
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.mapper.caption
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppTypographyKey

/** A list card with a divider under the texts; a null [image] is the placeholder of the same size. */
@Composable
internal fun ImageRow(image: SpaceImage?, onClick: () -> Unit, onFavorite: () -> Unit) {
    val gap = AppDimenKey.Padding.MEDIUM.dp()
    val start = AppDimenKey.Padding.LARGE.dp()
    Column(Modifier.fillMaxWidth().clickable(enabled = image != null, onClick = onClick)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(gap),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = start, end = AppDimenKey.Padding.EXTRA_SMALL.dp())
                .padding(vertical = AppDimenKey.Padding.SMALL.dp()),
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
        // From the start of the texts to the end of the screen.
        HorizontalDivider(
            color = AppColorKey.Border.DEFAULT.color(),
            modifier = Modifier.padding(start = start + THUMBNAIL_SIZE + gap),
        )
    }
}

/** A grid card; a null [image] is the placeholder of the same size. */
@Composable
internal fun ImageTile(image: SpaceImage?, onClick: () -> Unit, onFavorite: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimenKey.CornerRadius.MEDIUM.dp()))
            .clickable(enabled = image != null, onClick = onClick),
    ) {
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
            ImageTexts(image)
        }
    }
}

/** The title in two lines and "date · center" under it; grey bars for a placeholder. */
@Composable
private fun ImageTexts(image: SpaceImage?) {
    if (image == null) {
        TextBar(AppTypographyKey.BODY, lines = 2, widthFraction = 0.9f)
        TextBar(AppTypographyKey.CAPTION, lines = 1, widthFraction = 0.5f)
        return
    }
    Text(
        text = image.title,
        style = AppTypographyKey.BODY.style(),
        fontWeight = FontWeight.SemiBold,
        color = AppColorKey.Text.PRIMARY.color(),
        maxLines = 2,
        minLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    Text(
        text = image.caption(),
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

private val THUMBNAIL_SIZE = 72.dp
private val HEART_SIZE = 48.dp
