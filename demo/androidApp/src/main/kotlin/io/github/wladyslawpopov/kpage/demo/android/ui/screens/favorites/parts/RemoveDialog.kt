package io.github.wladyslawpopov.kpage.demo.android.ui.screens.favorites.parts

import androidx.compose.runtime.Composable
import io.github.wladyslawpopov.kpage.demo.android.ui.widgets.ConfirmDialog
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey

/** Asks whether to remove the images from the favorites. */
@Composable
internal fun RemoveDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    ConfirmDialog(
        title = AppStringKey.Favorites.REMOVE_TITLE,
        message = AppStringKey.Favorites.REMOVE_MESSAGE,
        confirmText = AppStringKey.Favorites.REMOVE,
        onConfirm = onConfirm,
        onCancel = onCancel,
    )
}
