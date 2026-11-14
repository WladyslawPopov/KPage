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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.ui.widgets.ConfirmDialog
import io.github.wladyslawpopov.kpage.demo.android.ui.widgets.DialogCancelButton
import io.github.wladyslawpopov.kpage.demo.shared.strings.asString
import io.github.wladyslawpopov.kpage.demo.uicore.testing.UiTestId
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey

/** The fields of a new chat; they belong to the screen, only the result goes out. */
@Composable
internal fun CreateChatDialog(onCreate: (title: String, interlocutor: String) -> Unit, onCancel: () -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var interlocutor by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancel,
        // A dialog is a window of its own: the flag of the activity's content does not reach it.
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        title = { Text(AppStringKey.Chat.CREATE_TITLE.asString()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimenKey.Padding.SMALL.dp())) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(MAX_LENGTH) },
                    label = { Text(AppStringKey.Chat.TITLE_HINT.asString()) },
                    singleLine = true,
                    modifier = Modifier.testTag(UiTestId.NEW_CHAT_TITLE.id),
                )
                OutlinedTextField(
                    value = interlocutor,
                    onValueChange = { interlocutor = it.take(MAX_LENGTH) },
                    label = { Text(AppStringKey.Chat.INTERLOCUTOR_HINT.asString()) },
                    singleLine = true,
                    modifier = Modifier.testTag(UiTestId.NEW_CHAT_INTERLOCUTOR.id),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(title, interlocutor) },
                enabled = title.isNotBlank() && interlocutor.isNotBlank(),
                modifier = Modifier.testTag(UiTestId.NEW_CHAT_CREATE.id),
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
