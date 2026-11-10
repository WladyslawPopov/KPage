package io.github.wladyslawpopov.kpage.demo.android.ui.screens.chat.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import io.github.wladyslawpopov.kpage.demo.android.theme.color
import io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.listing.mapper.ChatAvatar
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey

/** The first letter of the interlocutor on their color; a grey circle while there is no [avatar]. */
@Composable
internal fun ChatAvatarView(avatar: ChatAvatar?, size: Dp, textStyle: TextStyle) {
    Box(
        Modifier
            .size(size)
            .background(avatar?.let { AVATAR_PALETTE[it.color % AVATAR_PALETTE.size] } ?: AppColorKey.Container.SECONDARY.color(), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (avatar != null) Text(avatar.letter, style = textStyle, color = Color.White)
    }
}

// One color per index of ChatAvatar.color; the same order on iOS.
private val AVATAR_PALETTE = listOf(
    Color(0xFF5C6BC0), Color(0xFF26A69A), Color(0xFFEF6C00),
    Color(0xFFAB47BC), Color(0xFF42A5F5), Color(0xFFEC407A),
)
