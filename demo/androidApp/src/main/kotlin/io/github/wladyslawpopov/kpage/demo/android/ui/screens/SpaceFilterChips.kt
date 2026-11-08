package io.github.wladyslawpopov.kpage.demo.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceQuery
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.SpaceFilterOptions
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.SpaceIntent
import io.github.wladyslawpopov.kpage.demo.shared.strings.asString
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppTypographyKey

/** The same height as the buttons of the bar. */
private val CHIP_HEIGHT = 40.dp

/**
 * The filters that are set, as chips under the header, with no strip behind them. A tap on a chip
 * removes its filter; the years are one chip. Hidden while no filter is set.
 */
@Composable
fun SpaceFilterChips(query: SpaceQuery, onIntent: (SpaceIntent) -> Unit) {
    val isFiltered = query.center != null || query.yearStart != null || query.yearEnd != null
    AnimatedVisibility(
        visible = isFiltered,
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
            query.center?.let { center ->
                RemovableChip(
                    text = SpaceFilterOptions.title(center).asString(),
                    onRemove = { onIntent(SpaceIntent.SetFilters(null, query.yearStart, query.yearEnd)) },
                )
            }
            if (query.yearStart != null || query.yearEnd != null) {
                RemovableChip(
                    text = yearsText(query.yearStart, query.yearEnd),
                    onRemove = { onIntent(SpaceIntent.SetFilters(query.center, null, null)) },
                )
            }
        }
    }
}

/** A chip in the style of the bar buttons: the same highlight and height, no border. */
@Composable
private fun RemovableChip(text: String, onRemove: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimenKey.Padding.EXTRA_SMALL.dp()),
        modifier = Modifier
            .height(CHIP_HEIGHT)
            .clip(CircleShape)
            .background(barItemColor())
            .clickable(role = Role.Button, onClick = onRemove)
            .padding(start = AppDimenKey.Padding.LARGE.dp(), end = AppDimenKey.Padding.MEDIUM.dp()),
    ) {
        Text(
            text = text,
            style = AppTypographyKey.LABEL.style(),
            color = AppColorKey.Text.PRIMARY.color(),
            maxLines = 1,
        )
        Icon(
            imageVector = AppIconKey.CLOSE.vector(),
            contentDescription = AppStringKey.Core.CLEAR.asString(),
            tint = AppColorKey.Icon.SECONDARY.color(),
            modifier = Modifier.size(AppDimenKey.IconSize.SMALL.dp()),
        )
    }
}

/** "1960–1969", or "From 1960" / "To 1969" when only one end is set. */
@Composable
private fun yearsText(from: Int?, to: Int?): String = when {
    from != null && to != null -> "$from–$to"
    from != null -> "${AppStringKey.Space.FROM.asString()} $from"
    else -> "${AppStringKey.Space.TO.asString()} $to"
}
