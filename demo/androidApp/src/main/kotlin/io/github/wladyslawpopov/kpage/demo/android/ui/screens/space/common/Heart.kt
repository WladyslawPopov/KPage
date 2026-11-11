package io.github.wladyslawpopov.kpage.demo.android.ui.screens.space.common

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.wladyslawpopov.kpage.demo.android.theme.color
import io.github.wladyslawpopov.kpage.demo.android.theme.vector
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey

/** The favorite mark; over an image it gets a background to stay visible on any picture. */
@Composable
internal fun Heart(
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
            tint = if (isFavorite) AppColorKey.Icon.FAVORITE.color() else AppColorKey.Icon.SECONDARY.color(),
        )
    }
}
