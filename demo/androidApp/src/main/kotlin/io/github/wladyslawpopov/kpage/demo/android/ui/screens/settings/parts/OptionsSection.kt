package io.github.wladyslawpopov.kpage.demo.android.ui.screens.settings.parts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import io.github.wladyslawpopov.kpage.demo.android.theme.asString
import io.github.wladyslawpopov.kpage.demo.android.theme.color
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.style
import io.github.wladyslawpopov.kpage.demo.presentation.screens.settings.display.SettingsOption
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppTypographyKey

/** A titled group of choices, one of them selected. */
@Composable
internal fun <T : Any> OptionsSection(title: AppStringKey, options: List<SettingsOption<T>>, onSelect: (T) -> Unit) {
    Column {
        SectionTitle(title)
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = option.selected, role = Role.RadioButton) { onSelect(option.value) }
                    .padding(horizontal = AppDimenKey.Padding.MEDIUM.dp()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // The row takes the tap; the button only shows the choice.
                RadioButton(selected = option.selected, onClick = null)
                Text(
                    text = option.label.asString(),
                    style = AppTypographyKey.BODY.style(),
                    color = AppColorKey.Text.PRIMARY.color(),
                    modifier = Modifier.padding(
                        start = AppDimenKey.Padding.MEDIUM.dp(),
                        top = AppDimenKey.Padding.MEDIUM.dp(),
                        bottom = AppDimenKey.Padding.MEDIUM.dp(),
                    ),
                )
            }
        }
    }
}
