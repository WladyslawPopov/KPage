package io.github.wladyslawpopov.kpage.demo.android.ui.screens.space

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.wladyslawpopov.kpage.demo.android.theme.color
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.style
import io.github.wladyslawpopov.kpage.demo.android.theme.vector
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceCenter
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.SpaceFilterOptions
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.SpaceIntent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.SpaceState
import io.github.wladyslawpopov.kpage.demo.shared.strings.asString
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppTypographyKey
import java.text.NumberFormat

/**
 * The bottom sheet of the Space filters. A choice is applied at once, so the list under the sheet
 * changes and the button shows how many images the filters leave. Whether the sheet is open is the
 * state of the screen, not of the list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceFiltersSheet(
    state: SpaceState,
    onIntent: (SpaceIntent) -> Unit,
    onDismiss: () -> Unit,
) {
    val query = state.list.query
    val setFilters = { center: SpaceCenter?, from: Int?, to: Int? ->
        onIntent(SpaceIntent.SetFilters(center, from, to))
    }
    val first = SpaceFilterOptions.firstYear
    val last = SpaceFilterOptions.lastYear
    // The range cannot end before it starts: each menu offers only the years that keep it valid.
    val fromYears = (first..(query.yearEnd ?: last)).reversed().toList()
    val toYears = ((query.yearStart ?: first)..last).reversed().toList()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = AppDimenKey.Padding.LARGE.dp())
                .padding(bottom = AppDimenKey.Padding.EXTRA_LARGE.dp()),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = AppStringKey.Core.FILTERS.asString(),
                    style = AppTypographyKey.SUBTITLE.style(),
                    color = AppColorKey.Text.PRIMARY.color(),
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = { setFilters(null, null, null) },
                    enabled = state.isFiltered,
                ) { Text(AppStringKey.Core.RESET.asString()) }
            }
            FilterMenu(
                label = AppStringKey.Space.CENTER.asString(),
                value = query.center,
                options = SpaceFilterOptions.centers,
                optionText = { SpaceFilterOptions.title(it).asString() },
                onSelect = { setFilters(it, query.yearStart, query.yearEnd) },
            )
            HorizontalDivider(color = AppColorKey.Border.DEFAULT.color())
            FilterMenu(
                label = AppStringKey.Space.FROM.asString(),
                value = query.yearStart,
                options = fromYears,
                optionText = { it.toString() },
                onSelect = { setFilters(query.center, it, query.yearEnd) },
            )
            HorizontalDivider(color = AppColorKey.Border.DEFAULT.color())
            FilterMenu(
                label = AppStringKey.Space.TO.asString(),
                value = query.yearEnd,
                options = toYears,
                optionText = { it.toString() },
                onSelect = { setFilters(query.center, query.yearStart, it) },
            )
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppDimenKey.Padding.LARGE.dp()),
            ) { Text(showLabel(state)) }
        }
    }
}

/** "Show", and how many images the filters leave once the list has loaded. */
@Composable
private fun showLabel(state: SpaceState): String {
    val show = AppStringKey.Space.SHOW.asString()
    if (state.page.isLoading) return show
    return "$show ${NumberFormat.getIntegerInstance().format(state.list.totalCount)}"
}

/** A row with the chosen value; a tap opens the menu of [options] with "Any" on top. */
@Composable
private fun <T : Any> FilterMenu(
    label: String,
    value: T?,
    options: List<T>,
    optionText: @Composable (T) -> String,
    onSelect: (T?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val any = AppStringKey.Space.ANY.asString()
    Box {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(vertical = AppDimenKey.Padding.MEDIUM.dp()),
        ) {
            Text(label, style = AppTypographyKey.BODY.style(), color = AppColorKey.Text.PRIMARY.color())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value?.let { optionText(it) } ?: any,
                    style = AppTypographyKey.BODY.style(),
                    color = AppColorKey.Text.SECONDARY.color(),
                )
                Icon(
                    imageVector = AppIconKey.DROPDOWN.vector(),
                    contentDescription = null,
                    tint = AppColorKey.Icon.SECONDARY.color(),
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(any) },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionText(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
