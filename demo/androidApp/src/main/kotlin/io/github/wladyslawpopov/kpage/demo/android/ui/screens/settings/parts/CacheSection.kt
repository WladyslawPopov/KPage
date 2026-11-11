package io.github.wladyslawpopov.kpage.demo.android.ui.screens.settings.parts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.asString
import io.github.wladyslawpopov.kpage.demo.android.theme.color
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.style
import io.github.wladyslawpopov.kpage.demo.shared.strings.asString
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.text.UiText
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppTypographyKey

/** What the paginators keep on the device, and the button that clears it. */
@Composable
internal fun CacheSection(lists: UiText, size: UiText, onClear: () -> Unit) {
    Column {
        SectionTitle(AppStringKey.Settings.CACHE)
        CacheRow(AppStringKey.Settings.CACHE_LISTS, lists)
        CacheRow(AppStringKey.Settings.CACHE_SIZE, size)
        // Destructive, as the red button on iOS.
        val danger = AppColorKey.Status.FAIL.color()
        OutlinedButton(
            onClick = onClear,
            modifier = Modifier.padding(AppDimenKey.Padding.MEDIUM.dp()),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = danger),
            border = BorderStroke(1.dp, danger),
        ) { Text(AppStringKey.Settings.CLEAR_CACHE.asString()) }
    }
}

@Composable
private fun CacheRow(label: AppStringKey, value: UiText) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimenKey.Padding.MEDIUM.dp(), vertical = AppDimenKey.Padding.SMALL.dp()),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label.asString(), style = AppTypographyKey.BODY.style(), color = AppColorKey.Text.PRIMARY.color())
        Text(value.asString(), style = AppTypographyKey.BODY.style(), color = AppColorKey.Text.SECONDARY.color())
    }
}
