package io.github.wladyslawpopov.kpage.demo.android.ui.screens.chat.listing.parts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.ui.widgets.ConfirmDialog
import io.github.wladyslawpopov.kpage.demo.android.ui.widgets.DialogCancelButton
import io.github.wladyslawpopov.kpage.demo.shared.strings.asString
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey

/** The fields of a new chat; they belong to the screen, only the result goes out. */
@Composable
internal fun CreateChatDialog(onCreate: (title: String, interlocutor: String) -> Unit, onCancel: () -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var interlocutor by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(AppStringKey.Chat.CREATE_TITLE.asString()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimenKey.Padding.SMALL.dp())) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(MAX_LENGTH) },
                    label = { Text(AppStringKey.Chat.TITLE_HINT.asString()) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = interlocutor,
                    onValueChange = { interlocutor = it.take(MAX_LENGTH) },
                    label = { Text(AppStringKey.Chat.INTERLOCUTOR_HINT.asString()) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(title, interlocutor) },
                enabled = title.isNotBlank() && interlocutor.isNotBlank(),
            ) { Text(AppStringKey.Chat.CREATE.asString()) }
        },
        dismissButton = { DialogCancelButton(onCancel) },
    )
}

/** Asks whether to delete the chats. */
@Composable
internal fun DeleteChatsDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    ConfirmDialog(
        title = AppStringKey.Chat.DELETE_TITLE,
        message = AppStringKey.Chat.DELETE_MESSAGE,
        confirmText = AppStringKey.Chat.DELETE,
        onConfirm = onConfirm,
        onCancel = onCancel,
    )
}

private const val MAX_LENGTH = 40
