package io.github.wladyslawpopov.kpage.demo.android.ui.screens.chat.listing

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.wladyslawpopov.kpage.demo.android.ui.page.PageLayout
import io.github.wladyslawpopov.kpage.demo.android.ui.paging.PagedGrid
import io.github.wladyslawpopov.kpage.demo.android.ui.paging.rememberCanHideBars
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.chat.listing.parts.ChatRow
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.chat.listing.parts.ChatsTopBar
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.chat.listing.parts.CreateChatDialog
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.chat.listing.parts.DeleteChatsDialog
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.favorites.parts.SwipeToRemove
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.listing.ChatsComponent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.listing.ChatsIntent
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey

@Composable
fun ChatsScreen(component: ChatsComponent) {
    val state by component.state.collectAsState()
    val gridState = rememberLazyGridState()
    val canHideBars by rememberCanHideBars(gridState)

    // Back leaves the selection mode first.
    BackHandler(enabled = state.isSelecting) { component.onIntent(ChatsIntent.ClearSelection) }
    if (state.pendingDelete.isNotEmpty()) {
        DeleteChatsDialog(
            onConfirm = { component.onIntent(ChatsIntent.ConfirmDelete) },
            onCancel = { component.onIntent(ChatsIntent.CancelDelete) },
        )
    }
    if (state.isCreating) {
        CreateChatDialog(
            onCreate = { title, interlocutor -> component.onIntent(ChatsIntent.Create(title, interlocutor)) },
            onCancel = { component.onIntent(ChatsIntent.CancelCreate) },
        )
    }
    PageLayout(
        page = state.page,
        onPageIntent = component::onPageIntent,
        hideBottomBarOnScroll = canHideBars,
        header = { ChatsTopBar(state = state, onIntent = component::onIntent) },
    ) { padding ->
        PagedGrid(
            list = state.list,
            keyOf = { it.id },
            gridState = gridState,
            contentPadding = padding,
            spacing = AppDimenKey.Padding.NONE,
            placeholderContent = { ChatRow(null, onClick = {}, onLongClick = {}) },
            itemContent = { chat, _ ->
                SwipeToRemove(
                    enabled = !state.isSelecting,
                    isAsking = state.pendingDelete.isNotEmpty(),
                    onRemove = { component.onIntent(ChatsIntent.DeleteOne(chat.id)) },
                ) {
                    ChatRow(
                        chat = chat,
                        onClick = { component.onIntent(ChatsIntent.Click(chat.id)) },
                        onLongClick = { component.onIntent(ChatsIntent.LongClick(chat.id)) },
                    ) {
                        if (state.isSelecting) Checkbox(checked = chat.id in state.selected, onCheckedChange = null)
                    }
                }
            },
        )
    }
}
