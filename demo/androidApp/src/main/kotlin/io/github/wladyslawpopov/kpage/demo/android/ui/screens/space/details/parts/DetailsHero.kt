package io.github.wladyslawpopov.kpage.demo.android.ui.screens.space.details.parts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.wladyslawpopov.kpage.demo.android.theme.color
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.space.common.Heart
import io.github.wladyslawpopov.kpage.demo.android.ui.widgets.RemoteImage
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.details.display.SpaceDetailsDisplay
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.details.SpaceDetailsIntent
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey

/** The picture with the heart on it; a null [display] (not loaded yet) is an empty frame of the usual proportions. */
@Composable
internal fun DetailsHero(display: SpaceDetailsDisplay?, onIntent: (SpaceDetailsIntent) -> Unit, onOpen: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(display?.picture?.aspectRatio ?: DEFAULT_RATIO)
            .background(AppColorKey.Container.SECONDARY.color())
            .clickable(enabled = display?.fullPictureUrl != null, onClick = onOpen),
    ) {
        if (display != null) {
            RemoteImage(url = display.picture.url, contentDescription = display.title, modifier = Modifier.fillMaxSize())
            Heart(
                isFavorite = display.isFavorite,
                onClick = { onIntent(SpaceDetailsIntent.SetFavorite(!display.isFavorite)) },
                onImage = true,
                modifier = Modifier.align(Alignment.BottomEnd).padding(AppDimenKey.Padding.MEDIUM.dp()),
            )
        }
    }
}

private const val DEFAULT_RATIO = 4f / 3
