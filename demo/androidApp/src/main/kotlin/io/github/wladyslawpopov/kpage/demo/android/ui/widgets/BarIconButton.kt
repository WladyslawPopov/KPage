package io.github.wladyslawpopov.kpage.demo.android.ui.widgets

import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.wladyslawpopov.kpage.demo.android.theme.color
import io.github.wladyslawpopov.kpage.demo.android.ui.barItemColor
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey

/** An icon button of a bar without a background: a round highlight keeps it visible over the content. */
@Composable
fun BarIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = barItemColor(),
            contentColor = AppColorKey.Icon.PRIMARY.color(),
        ),
        content = content,
    )
}
