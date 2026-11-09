package io.github.wladyslawpopov.kpage.demo.android.ui.screens.space.listing.filters

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.asString
import io.github.wladyslawpopov.kpage.demo.android.theme.color
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.style
import io.github.wladyslawpopov.kpage.demo.android.theme.vector
import io.github.wladyslawpopov.kpage.demo.android.ui.barItemColor
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.display.SpaceChip
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.display.SpaceChipTarget
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.SpaceIntent
import io.github.wladyslawpopov.kpage.demo.shared.strings.asString
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppTypographyKey

/** Lower than the buttons of the bar: a chip is a small label, not a button. */
private val CHIP_HEIGHT = 32.dp

/** A long text (a photographer, an album) is cut, so one chip does not take the whole row. */
private val CHIP_MAX_TEXT_WIDTH = 160.dp

/**
 * The [chips] of what is set, under the header, with no strip behind them. A tap on a chip opens what
 * it came from (the search field, the filters); its cross removes it. The search chip is hidden while
 * the field is open; the row is hidden while there is nothing to show.
 */
@Composable
fun SpaceFilterChips(
    chips: List<SpaceChip>,
    isSearchOpen: Boolean,
    onOpenSearch: () -> Unit,
    onOpenFilters: () -> Unit,
    onIntent: (SpaceIntent) -> Unit,
) {
    val shown = chips.filter { !(it.opens == SpaceChipTarget.SEARCH && isSearchOpen) }
    AnimatedVisibility(
        visible = shown.isNotEmpty(),
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
            shown.forEach { chip ->
                BarChip(
                    text = chip.text.asString(),
                    leadingIcon = chip.icon,
                    onClick = if (chip.opens == SpaceChipTarget.SEARCH) onOpenSearch else onOpenFilters,
                    onRemove = { onIntent(chip.remove) },
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
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = CHIP_MAX_TEXT_WIDTH),
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
