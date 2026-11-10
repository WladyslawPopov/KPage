package io.github.wladyslawpopov.kpage.demo.android.ui.screens.favorites.parts

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import io.github.wladyslawpopov.kpage.demo.shared.strings.asString
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey

/** Asks whether to remove the images from the favorites. */
@Composable
internal fun RemoveDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(AppStringKey.Favorites.REMOVE_TITLE.asString()) },
        text = { Text(AppStringKey.Favorites.REMOVE_MESSAGE.asString()) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(AppStringKey.Favorites.REMOVE.asString()) } },
        dismissButton = { TextButton(onClick = onCancel) { Text(AppStringKey.Core.CANCEL.asString()) } },
    )
}
