package io.github.wladyslawpopov.kpage.demo.android.ui.screens.chat.listing.parts

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.asString
import io.github.wladyslawpopov.kpage.demo.android.theme.color
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.style
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.chat.common.ChatAvatarView
import io.github.wladyslawpopov.kpage.demo.domain.chat.Chat
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.listing.mapper.avatar
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.listing.mapper.time
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppTypographyKey

/**
 * A chat of the list: the avatar, the title, the last message and its time; a null [chat] is the
 * placeholder of the same size.
 *
 * @param trailing The end of the row: a checkbox in the selection mode.
 */
@Composable
internal fun ChatRow(
    chat: Chat?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    val haptics = LocalHapticFeedback.current
    val start = AppDimenKey.Padding.LARGE.dp()
    val gap = AppDimenKey.Padding.MEDIUM.dp()
    Column(
        Modifier.fillMaxWidth().combinedClickable(
            enabled = chat != null,
            onClick = onClick,
            onLongClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongClick()
            },
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(gap),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = start, vertical = AppDimenKey.Padding.MEDIUM.dp()),
        ) {
            ChatAvatarView(chat?.avatar(), AVATAR_SIZE, AppTypographyKey.TITLE.style())
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (chat == null) {
                    Bar(0.6f)
                    Bar(0.9f)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = chat.title,
                            style = AppTypographyKey.SUBTITLE.style(),
                            fontWeight = FontWeight.SemiBold,
                            color = AppColorKey.Text.PRIMARY.color(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = chat.time().asString(),
                            style = AppTypographyKey.CAPTION.style(),
                            color = AppColorKey.Text.SECONDARY.color(),
                        )
                    }
                    Text(
                        text = chat.lastMessage,
                        style = AppTypographyKey.BODY.style(),
                        color = AppColorKey.Text.SECONDARY.color(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            trailing()
        }
        HorizontalDivider(
            color = AppColorKey.Border.DEFAULT.color(),
            modifier = Modifier.padding(start = start + AVATAR_SIZE + gap),
        )
    }
}

@Composable
private fun Bar(widthFraction: Float) {
    Box(
        Modifier
            .fillMaxWidth(widthFraction)
            .height(AppDimenKey.IconSize.EXTRA_SMALL.dp())
            .background(AppColorKey.Container.SECONDARY.color(), RoundedCornerShape(AppDimenKey.CornerRadius.SMALL.dp())),
    )
}

private val AVATAR_SIZE = 48.dp
