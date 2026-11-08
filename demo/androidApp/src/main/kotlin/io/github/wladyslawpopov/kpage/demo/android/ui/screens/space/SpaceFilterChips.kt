package io.github.wladyslawpopov.kpage.demo.android.ui.screens.space

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.color
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.style
import io.github.wladyslawpopov.kpage.demo.android.theme.vector
import io.github.wladyslawpopov.kpage.demo.android.ui.barItemColor
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.SpaceFilterOptions
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.SpaceIntent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.SpaceState
import io.github.wladyslawpopov.kpage.demo.shared.strings.asString
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppTypographyKey

/** Lower than the buttons of the bar: a chip is a small label, not a button. */
private val CHIP_HEIGHT = 32.dp

/**
 * What is set, as chips under the header, with no strip behind them: the search text while the field
 * is closed, the center, the years (one chip). A tap on a chip opens what it came from (the search
 * field, the filters); its cross removes it. Hidden while nothing is set.
 */
@Composable
fun SpaceFilterChips(
    state: SpaceState,
    isSearchOpen: Boolean,
    onOpenSearch: () -> Unit,
    onOpenFilters: () -> Unit,
    onIntent: (SpaceIntent) -> Unit,
) {
    val query = state.list.query
    val searchText = state.searchText.trim()
    val showSearch = searchText.isNotEmpty() && !isSearchOpen
    AnimatedVisibility(
        visible = showSearch || state.isFiltered,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppDimenKey.Padding.SMALL.dp()),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = AppDimenKey.Padding.LARGE.dp(), vertical = AppDimenKey.Padding.EXTRA_SMALL.dp()),
        ) {
            if (showSearch) {
                BarChip(
                    text = searchText,
                    leadingIcon = AppIconKey.SEARCH,
                    onClick = onOpenSearch,
                    onRemove = { onIntent(SpaceIntent.SearchChanged("")) },
                )
            }
            query.center?.let { center ->
                BarChip(
                    text = SpaceFilterOptions.title(center).asString(),
                    onClick = onOpenFilters,
                    onRemove = { onIntent(SpaceIntent.SetFilters(null, query.yearStart, query.yearEnd)) },
                )
            }
            if (query.yearStart != null || query.yearEnd != null) {
                BarChip(
                    text = yearsText(query.yearStart, query.yearEnd),
                    onClick = onOpenFilters,
                    onRemove = { onIntent(SpaceIntent.SetFilters(query.center, null, null)) },
                )
            }
        }
    }
}

/**
 * A chip in the style of the bar buttons: the same highlight, no border, smaller text and height. The text opens
 * [onClick]; the cross is a separate target that calls [onRemove].
 */
@Composable
private fun BarChip(
    text: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    leadingIcon: AppIconKey? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(CHIP_HEIGHT)
            .clip(CircleShape)
            .background(barItemColor()),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimenKey.Padding.EXTRA_SMALL.dp()),
            modifier = Modifier
                .fillMaxHeight()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(start = AppDimenKey.Padding.MEDIUM.dp()),
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon.vector(),
                    contentDescription = null,
                    tint = AppColorKey.Icon.SECONDARY.color(),
                    modifier = Modifier.size(AppDimenKey.IconSize.SMALL.dp()),
                )
            }
            Text(
                text = text,
                style = AppTypographyKey.CAPTION.style(),
                color = AppColorKey.Text.PRIMARY.color(),
                maxLines = 1,
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(CHIP_HEIGHT)
                .clickable(role = Role.Button, onClick = onRemove),
        ) {
            Icon(
                imageVector = AppIconKey.CLOSE.vector(),
                contentDescription = AppStringKey.Core.CLEAR.asString(),
                tint = AppColorKey.Icon.SECONDARY.color(),
                modifier = Modifier.size(AppDimenKey.IconSize.SMALL.dp()),
            )
        }
    }
}

/** "1960–1969", or "From 1960" / "To 1969" when only one end is set. */
@Composable
private fun yearsText(from: Int?, to: Int?): String = when {
    from != null && to != null -> "$from–$to"
    from != null -> "${AppStringKey.Space.FROM.asString()} $from"
    else -> "${AppStringKey.Space.TO.asString()} $to"
}
