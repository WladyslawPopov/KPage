package io.github.wladyslawpopov.kpage.demo.android.ui.screens.chat.room.parts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.asString
import io.github.wladyslawpopov.kpage.demo.android.theme.color
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.style
import io.github.wladyslawpopov.kpage.demo.uicore.text.UiText
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppTypographyKey

/** The day of the messages under it, in the middle of the list. */
@Composable
internal fun DateSeparator(date: UiText) {
    Box(Modifier.fillMaxWidth().padding(vertical = AppDimenKey.Padding.SMALL.dp()), contentAlignment = Alignment.Center) {
        Text(
            text = date.asString(),
            style = AppTypographyKey.CAPTION.style(),
            color = AppColorKey.Text.SECONDARY.color(),
            modifier = Modifier
                .background(AppColorKey.Container.ELEVATED.color(), CircleShape)
                .padding(horizontal = AppDimenKey.Padding.MEDIUM.dp(), vertical = AppDimenKey.Padding.EXTRA_SMALL.dp()),
        )
    }
}

/** A message that is not loaded yet: a grey bubble, on alternating sides. */
@Composable
internal fun MessagePlaceholder(serverIndex: Int) {
    Box(
        Modifier.fillMaxWidth().padding(horizontal = AppDimenKey.Padding.SMALL.dp(), vertical = 2.dp),
        contentAlignment = if (serverIndex % 2 == 0) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Box(
            Modifier
                .width(200.dp)
                .height(48.dp)
                .background(AppColorKey.Container.SECONDARY.color(), RoundedCornerShape(18.dp)),
        )
    }
}
