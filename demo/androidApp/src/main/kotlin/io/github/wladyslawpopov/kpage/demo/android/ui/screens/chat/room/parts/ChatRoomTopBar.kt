package io.github.wladyslawpopov.kpage.demo.android.ui.screens.chat.room.parts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.color
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.style
import io.github.wladyslawpopov.kpage.demo.android.theme.vector
import io.github.wladyslawpopov.kpage.demo.android.ui.barColor
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.chat.common.ChatAvatarView
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room.ChatRoomIntent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room.ChatRoomState
import io.github.wladyslawpopov.kpage.demo.shared.strings.asString
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppTypographyKey

/**
 * The avatar and the title of the chat with the interlocutor under it, the menu of the chat, and how
 * the mock behind them behaves.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatRoomTopBar(state: ChatRoomState, onBack: () -> Unit, onIntent: (ChatRoomIntent) -> Unit) {
    Column(Modifier.background(barColor())) {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimenKey.Padding.MEDIUM.dp()),
                ) {
                    ChatAvatarView(state.avatar, AVATAR_SIZE, AppTypographyKey.SUBTITLE.style())
                    Column {
                        Text(
                            state.title,
                            style = AppTypographyKey.SUBTITLE.style(),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (state.interlocutor.isNotEmpty()) {
                            Text(
                                state.interlocutor,
                                style = AppTypographyKey.BODY.style(),
                                color = AppColorKey.Text.SECONDARY.color(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(AppIconKey.BACK.vector(), contentDescription = AppStringKey.Core.BACK.asString())
                }
            },
            actions = { if (state.actions.isNotEmpty()) ActionsMenu(state, onIntent) },
            windowInsets = WindowInsets(0),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
            ),
        )
        state.scenarioNote?.let { note ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimenKey.Padding.EXTRA_SMALL.dp()),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimenKey.Padding.LARGE.dp())
                    .padding(bottom = AppDimenKey.Padding.EXTRA_SMALL.dp()),
            ) {
                Icon(
                    AppIconKey.INFO.vector(),
                    contentDescription = null,
                    tint = AppColorKey.Icon.SECONDARY.color(),
                    modifier = Modifier.size(AppDimenKey.IconSize.EXTRA_SMALL.dp()),
                )
                Text(
                    note.asString(),
                    style = AppTypographyKey.CAPTION.style(),
                    color = AppColorKey.Text.SECONDARY.color(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** The menu of the chat; its entries come ready from the shared code. */
@Composable
private fun ActionsMenu(state: ChatRoomState, onIntent: (ChatRoomIntent) -> Unit) {
    var isOpen by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { isOpen = true }) {
            Icon(AppIconKey.MORE.vector(), contentDescription = AppStringKey.Chat.ACTIONS.asString())
        }
        DropdownMenu(expanded = isOpen, onDismissRequest = { isOpen = false }) {
            state.actions.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.title.asString()) },
                    leadingIcon = { Icon(action.icon.vector(), contentDescription = null) },
                    onClick = {
                        isOpen = false
                        onIntent(action.intent)
                    },
                )
            }
        }
    }
}

private val AVATAR_SIZE = 40.dp
