package io.github.wladyslawpopov.kpage.demo.android.ui.screens.chat.room.parts

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.color
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.style
import io.github.wladyslawpopov.kpage.demo.android.theme.vector
import io.github.wladyslawpopov.kpage.demo.android.ui.barColor
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.chat.room.photos.savePhotos
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.chat.room.photos.url
import io.github.wladyslawpopov.kpage.demo.android.ui.widgets.RemoteImage
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room.ChatRoomIntent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.room.ChatRoomState
import io.github.wladyslawpopov.kpage.demo.shared.strings.asString
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppTypographyKey
import kotlinx.coroutines.launch

/**
 * The field at the bottom of the room: the photos to send, the photo picker, the text, the send
 * button. Its background reaches the bottom edge of the screen; [bottomInset] keeps the controls above
 * the system navigation bar.
 */
@Composable
internal fun Composer(
    state: ChatRoomState,
    onIntent: (ChatRoomIntent) -> Unit,
    bottomInset: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(ChatRoomState.MAX_ATTACHMENTS),
    ) { uris ->
        if (uris.isNotEmpty()) scope.launch { onIntent(ChatRoomIntent.AddPhotos(savePhotos(context, uris))) }
    }
    val corner = RoundedCornerShape(AppDimenKey.CornerRadius.LARGE.dp())
    Column(modifier.fillMaxWidth().background(barColor()).padding(bottom = bottomInset)) {
        if (state.attachments.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(AppDimenKey.Padding.SMALL.dp()),
                contentPadding = PaddingValues(
                    start = AppDimenKey.Padding.SMALL.dp(),
                    end = AppDimenKey.Padding.SMALL.dp(),
                    top = AppDimenKey.Padding.SMALL.dp(),
                ),
            ) {
                items(state.attachments, key = { it.path }) { photo ->
                    Box {
                        RemoteImage(photo.url(), contentDescription = null, modifier = Modifier.size(80.dp).clip(corner))
                        Icon(
                            AppIconKey.CLOSE.vector(),
                            contentDescription = AppStringKey.Core.CLEAR.asString(),
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(AppDimenKey.Padding.EXTRA_SMALL.dp())
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .clickable { onIntent(ChatRoomIntent.RemovePhoto(photo)) }
                                .padding(2.dp)
                                .size(AppDimenKey.IconSize.SMALL.dp()),
                        )
                    }
                }
            }
        }
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(horizontal = AppDimenKey.Padding.EXTRA_SMALL.dp(), vertical = AppDimenKey.Padding.EXTRA_SMALL.dp()),
        ) {
            IconButton(
                enabled = state.canAttach,
                onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            ) {
                Icon(AppIconKey.ATTACH.vector(), contentDescription = null)
            }
            // A plain field: the Material one keeps its own large paddings and height.
            val textStyle = AppTypographyKey.BODY.style().copy(color = AppColorKey.Text.PRIMARY.color())
            BasicTextField(
                value = state.text,
                onValueChange = { onIntent(ChatRoomIntent.TextChanged(it)) },
                textStyle = textStyle,
                maxLines = 5,
                cursorBrush = SolidColor(AppColorKey.Text.ACCENT.color()),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .padding(vertical = AppDimenKey.Padding.EXTRA_SMALL.dp())
                    .background(AppColorKey.Container.BACKGROUND.color(), corner),
                decorationBox = { field ->
                    Box(
                        Modifier.padding(horizontal = AppDimenKey.Padding.MEDIUM.dp(), vertical = AppDimenKey.Padding.SMALL.dp()),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (state.text.isEmpty()) {
                            Text(AppStringKey.Chat.MESSAGE_HINT.asString(), style = textStyle, color = AppColorKey.Text.SECONDARY.color())
                        }
                        field()
                    }
                },
            )
            IconButton(enabled = state.canSend, onClick = { onIntent(ChatRoomIntent.Send) }) {
                Icon(
                    AppIconKey.SEND.vector(),
                    contentDescription = null,
                    tint = if (state.canSend) AppColorKey.Icon.ACCENT.color() else AppColorKey.Overlay.DISABLED.color(),
                )
            }
        }
    }
}
