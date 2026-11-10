package io.github.wladyslawpopov.kpage.demo.android.ui.screens.chat.room.parts

import android.content.ClipData
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.asString
import io.github.wladyslawpopov.kpage.demo.android.theme.color
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.style
import io.github.wladyslawpopov.kpage.demo.android.theme.vector
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.chat.room.photos.url
import io.github.wladyslawpopov.kpage.demo.android.ui.widgets.RemoteImage
import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatAttachment
import io.github.wladyslawpopov.kpage.demo.domain.chat.MessageSendState
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room.ChatRoomIntent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room.display.ChatMessageItem
import io.github.wladyslawpopov.kpage.demo.shared.strings.asString
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppTypographyKey
import kotlinx.coroutines.launch

/**
 * A message: a bubble at the end for mine, at the start for the interlocutor's. A long press opens its
 * menu; the actions come ready from the shared code.
 */
@Composable
internal fun MessageBubble(
    item: ChatMessageItem,
    onIntent: (ChatRoomIntent) -> Unit,
    onOpenPhoto: (ChatAttachment) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val large = 18.dp
    val small = 4.dp
    val shape = if (item.isMine) {
        RoundedCornerShape(topStart = large, topEnd = large, bottomEnd = small, bottomStart = large)
    } else {
        RoundedCornerShape(topStart = large, topEnd = large, bottomEnd = large, bottomStart = small)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimenKey.Padding.SMALL.dp(), vertical = 2.dp),
        horizontalArrangement = if (item.isMine) Arrangement.End else Arrangement.Start,
    ) {
        Box {
            Surface(
                shape = shape,
                color = if (item.isMine) AppColorKey.Container.ACCENT.color() else AppColorKey.Container.ELEVATED.color(),
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(shape)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            if (item.actions.isNotEmpty()) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                showMenu = true
                            }
                        },
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(AppDimenKey.Padding.SMALL.dp()),
                    verticalArrangement = Arrangement.spacedBy(AppDimenKey.Padding.EXTRA_SMALL.dp()),
                ) {
                    if (item.attachments.isNotEmpty()) PhotosGrid(item.attachments, onOpenPhoto)
                    if (item.text.isNotEmpty()) {
                        Text(item.text, style = AppTypographyKey.BODY.style(), color = AppColorKey.Text.PRIMARY.color())
                    }
                    Footer(item)
                }
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                item.actions.forEach { action ->
                    DropdownMenuItem(
                        text = { Text(action.title.asString()) },
                        leadingIcon = { Icon(action.icon.vector(), contentDescription = null) },
                        onClick = {
                            showMenu = false
                            val copyText = action.copyText
                            if (copyText != null) {
                                scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(null, copyText))) }
                            }
                            onIntent(action.intent)
                        },
                    )
                }
            }
        }
    }
}

/** The time, and for a local message whether it failed. */
@Composable
private fun ColumnScope.Footer(item: ChatMessageItem) {
    Row(
        modifier = Modifier.align(Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimenKey.Padding.EXTRA_SMALL.dp()),
    ) {
        if (item.sendState == MessageSendState.FAILED) {
            Icon(
                AppIconKey.WARNING.vector(),
                contentDescription = null,
                tint = AppColorKey.Status.FAIL.color(),
                modifier = Modifier.size(AppDimenKey.IconSize.SMALL.dp()),
            )
            Text(
                AppStringKey.Chat.SEND_FAILED.asString(),
                style = AppTypographyKey.CAPTION.style(),
                color = AppColorKey.Status.FAIL.color(),
            )
        }
        Text(item.time.asString(), style = AppTypographyKey.CAPTION.style(), color = AppColorKey.Text.SECONDARY.color())
        if (item.isMine && item.sendState == null) {
            Icon(
                AppIconKey.DONE.vector(),
                contentDescription = null,
                tint = AppColorKey.Icon.ACCENT.color(),
                modifier = Modifier.size(AppDimenKey.IconSize.SMALL.dp()),
            )
        }
    }
}

// Two photos in a row; a single one keeps its own proportions.
@Composable
private fun PhotosGrid(photos: List<ChatAttachment>, onOpen: (ChatAttachment) -> Unit) {
    val corner = RoundedCornerShape(AppDimenKey.CornerRadius.MEDIUM.dp())
    val gap = AppDimenKey.Padding.EXTRA_SMALL.dp()
    Column(Modifier.widthIn(max = 284.dp), verticalArrangement = Arrangement.spacedBy(gap)) {
        if (photos.size == 1) {
            val photo = photos.single()
            val ratio = (photo.width.toFloat() / photo.height.coerceAtLeast(1)).coerceIn(0.5f, 2f)
            RemoteImage(
                photo.url(),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().aspectRatio(ratio).clip(corner).clickable { onOpen(photo) },
            )
        } else {
            photos.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    pair.forEach { photo ->
                        RemoteImage(
                            photo.url(),
                            contentDescription = null,
                            modifier = Modifier.size(140.dp).clip(corner).clickable { onOpen(photo) },
                        )
                    }
                }
            }
        }
    }
}
