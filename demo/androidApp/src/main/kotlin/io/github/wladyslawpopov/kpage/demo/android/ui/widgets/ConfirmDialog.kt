package io.github.wladyslawpopov.kpage.demo.android.ui.widgets

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import io.github.wladyslawpopov.kpage.demo.android.theme.color
import io.github.wladyslawpopov.kpage.demo.shared.strings.asString
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey

/**
 * A question with two buttons. The cancel button is plain; the confirm button is red, because most
 * of these dialogs ask to delete something. Pass another [confirmColor] for a harmless action.
 */
@Composable
fun ConfirmDialog(
    title: AppStringKey,
    message: AppStringKey,
    confirmText: AppStringKey,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    confirmColor: AppColorKey = AppColorKey.Status.FAIL,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title.asString()) },
        text = { Text(message.asString()) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = confirmColor.color()),
            ) { Text(confirmText.asString()) }
        },
        dismissButton = { DialogCancelButton(onCancel) },
    )
}

/** The button that closes a dialog without doing anything: the regular text color, not the accent. */
@Composable
fun DialogCancelButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(contentColor = AppColorKey.Text.PRIMARY.color()),
    ) { Text(AppStringKey.Core.CANCEL.asString()) }
}
