package io.github.wladyslawpopov.kpage.demo.android.ui.screens.space.details.parts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.wladyslawpopov.kpage.demo.android.theme.color
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.style
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.details.display.DetailsSection
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.details.display.SpaceDetailsDisplay
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.details.SpaceDetailsIntent
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppTypographyKey

/** Under the picture: the title, the description and the cards, exactly as [display] lists them. */
@Composable
internal fun DetailsContent(
    display: SpaceDetailsDisplay,
    onIntent: (SpaceDetailsIntent) -> Unit,
    onCopied: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AppDimenKey.Padding.LARGE.dp()),
        modifier = Modifier.padding(AppDimenKey.Padding.LARGE.dp()),
    ) {
        CopyableTitle(display.title, onCopied)
        display.description?.let { description ->
            // A long press selects words: the text can be copied in parts.
            SelectionContainer {
                Text(
                    text = description,
                    style = AppTypographyKey.BODY.style(),
                    color = AppColorKey.Text.PRIMARY.color(),
                )
            }
        }
        display.sections.forEach { section ->
            DetailsCard(section.title) {
                when (section) {
                    is DetailsSection.Fields -> DetailsFieldRows(section.rows, onIntent)
                    is DetailsSection.Chips -> DetailsChips(section.chips, onIntent)
                }
            }
        }
    }
}
