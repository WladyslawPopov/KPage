package io.github.wladyslawpopov.kpage.demo.android.ui.screens.settings.parts

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.wladyslawpopov.kpage.demo.android.theme.color
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.style
import io.github.wladyslawpopov.kpage.demo.shared.strings.asString
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppTypographyKey

@Composable
internal fun SectionTitle(title: AppStringKey) {
    Text(
        text = title.asString(),
        style = AppTypographyKey.LABEL.style(),
        color = AppColorKey.Text.SECONDARY.color(),
        modifier = Modifier.padding(
            start = AppDimenKey.Padding.MEDIUM.dp(),
            end = AppDimenKey.Padding.MEDIUM.dp(),
            top = AppDimenKey.Padding.LARGE.dp(),
            bottom = AppDimenKey.Padding.SMALL.dp(),
        ),
    )
}
