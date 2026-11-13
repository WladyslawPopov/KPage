package io.github.wladyslawpopov.kpage.demo.android.ui.screens.chat.room

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.ui.HideTabBar
import io.github.wladyslawpopov.kpage.demo.android.ui.LocalPageTopPadding
import io.github.wladyslawpopov.kpage.demo.android.ui.SystemBarStrip
import io.github.wladyslawpopov.kpage.demo.android.ui.page.PageLayout
import io.github.wladyslawpopov.kpage.demo.android.ui.paging.PagedGrid
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.chat.room.parts.ChatRoomTopBar
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.chat.room.parts.Composer
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.chat.room.parts.DateSeparator
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.chat.room.parts.MessageBubble
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.chat.room.parts.MessagePlaceholder
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.chat.room.photos.url
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.space.details.viewer.SpaceImageViewer
import io.github.wladyslawpopov.kpage.demo.android.ui.widgets.ConfirmDialog
import io.github.wladyslawpopov.kpage.demo.android.ui.widgets.rememberBackAction
import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatAttachment
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room.ChatRoomComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room.ChatRoomIntent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room.display.ChatDecoration
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room.display.rowKey
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * A chat: the reversed list of messages (row 0 is at the bottom) with the field over its end, without
 * the tab bar. When the bottom row changes (a new message, mine or the interlocutor's), the list goes
 * back to the bottom; the first show already starts there. A tap on the messages hides the keyboard,
 * a tap on a photo opens it full screen.
 */
@Composable
fun ChatRoomScreen(component: ChatRoomComponent) {
    HideTabBar()
    val state by component.state.collectAsState()
    val gridState = rememberLazyGridState()
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    var composerHeight by remember { mutableStateOf(0.dp) }
    // Shown only by this screen, so not a part of the state.
    var viewerUrl by rememberSaveable { mutableStateOf<String?>(null) }
    val openPhoto: (ChatAttachment) -> Unit = { viewerUrl = it.url() }

    val newestKey = state.newestKey
    var lastNewestKey by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(newestKey) {
        if (newestKey == null || newestKey == lastNewestKey) return@LaunchedEffect
        if (lastNewestKey != null) {
            delay(SCROLL_DELAY)
            gridState.scrollToItem(0)
        }
        lastNewestKey = newestKey
    }

    // Over the system navigation bar the field keeps its controls above it; the keyboard covers it.
    val isImeVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp
    val systemNavBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    viewerUrl?.let { url -> SpaceImageViewer(url = url, onClose = { viewerUrl = null }) }
    if (state.isDeleteAsked) DeleteChatDialog(onIntent = component::onIntent)

    Box(Modifier.fillMaxSize()) {
        PageLayout(
            page = state.page,
            onPageIntent = component::onPageIntent,
            header = {
                ChatRoomTopBar(
                    state = state,
                    onBack = rememberBackAction(),
                    onIntent = component::onIntent
                )
            },
        ) { padding ->
            // A tap on the messages hides the keyboard, as in MarketKMP.
            Box(
                Modifier.fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { focusManager.clearFocus() })
                    }
            ) {
                PagedGrid(
                    list = state.list,
                    keyOf = { it.rowKey() },
                    reversed = true,
                    showPosition = false,
                    spacing = AppDimenKey.Padding.NONE,
                    gridState = gridState,
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding(),
                        bottom = composerHeight + AppDimenKey.Padding.SMALL.dp(),
                    ),
                    decorationKeyOf = { decoration, _ ->
                        when (decoration) {
                            is ChatDecoration.Local -> decoration.item.key
                            is ChatDecoration.DateSeparator -> decoration.key
                        }
                    },
                    decorationContent = { decoration ->
                        when (decoration) {
                            is ChatDecoration.Local -> MessageBubble(
                                decoration.item,
                                component::onIntent,
                                openPhoto
                            )

                            is ChatDecoration.DateSeparator -> DateSeparator(decoration.date)
                        }
                    },
                    placeholderContent = { MessagePlaceholder(it) },
                    itemContent = { message, _ ->
                        val item = remember(message) { component.messageItem(message) }
                        MessageBubble(item, component::onIntent, openPhoto)
                    },
                )
                Composer(
                    state = state,
                    onIntent = component::onIntent,
                    bottomInset = if (isImeVisible) 0.dp else systemNavBar,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged {
                            composerHeight = with(density) {
                                it.height.toDp()
                            }
                        },
                )
            }
        }
        // The bar of the chat reaches the top edge: the messages do not show under the status bar.
        SystemBarStrip(LocalPageTopPadding.current, Modifier.align(Alignment.TopCenter))
    }
}

/** Asks whether to delete the whole chat. */
@Composable
private fun DeleteChatDialog(onIntent: (ChatRoomIntent) -> Unit) {
    ConfirmDialog(
        title = AppStringKey.Chat.DELETE_CHAT_TITLE,
        message = AppStringKey.Chat.DELETE_CHAT_MESSAGE,
        confirmText = AppStringKey.Chat.DELETE,
        onConfirm = { onIntent(ChatRoomIntent.ConfirmDeleteChat) },
        onCancel = { onIntent(ChatRoomIntent.CancelDeleteChat) },
    )
}

private val SCROLL_DELAY = 100.milliseconds
