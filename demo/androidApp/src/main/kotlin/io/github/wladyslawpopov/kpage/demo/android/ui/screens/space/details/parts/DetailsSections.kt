package io.github.wladyslawpopov.kpage.demo.android.ui.screens.space.details.parts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.details.display.DetailsChip
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.details.display.DetailsRow
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.details.SpaceDetailsIntent
import io.github.wladyslawpopov.kpage.demo.shared.strings.asString
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppTypographyKey

/** A titled card. */
@Composable
internal fun DetailsCard(title: AppStringKey, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimenKey.Padding.SMALL.dp())) {
        Text(
            text = title.asString(),
            style = AppTypographyKey.SUBTITLE.style(),
            color = AppColorKey.Text.PRIMARY.color(),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppDimenKey.CornerRadius.LARGE.dp()))
                .background(AppColorKey.Container.ELEVATED.color()),
            content = content,
        )
    }
}

/** The fields of a card with dividers between them; a row with an action is tappable and sends it. */
@Composable
internal fun DetailsFieldRows(rows: List<DetailsRow>, onIntent: (SpaceDetailsIntent) -> Unit) {
    rows.forEachIndexed { index, row ->
        if (index > 0) {
            HorizontalDivider(
                color = AppColorKey.Border.DEFAULT.color(),
                modifier = Modifier.padding(start = AppDimenKey.Padding.LARGE.dp()),
            )
        }
        val action = row.action
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimenKey.Padding.MEDIUM.dp()),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = action != null, role = Role.Button) { action?.let(onIntent) }
                .padding(horizontal = AppDimenKey.Padding.LARGE.dp(), vertical = AppDimenKey.Padding.MEDIUM.dp()),
        ) {
            row.icon?.let { icon ->
                Icon(
                    imageVector = icon.vector(),
                    contentDescription = null,
                    tint = AppColorKey.Icon.SECONDARY.color(),
                    modifier = Modifier.size(AppDimenKey.IconSize.MEDIUM.dp()),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = row.label.asString(),
                    style = AppTypographyKey.CAPTION.style(),
                    color = AppColorKey.Text.SECONDARY.color(),
                )
                Text(
                    text = row.value.asString(),
                    style = AppTypographyKey.BODY.style(),
                    color = if (action != null) AppColorKey.Text.ACCENT.color() else AppColorKey.Text.PRIMARY.color(),
                )
            }
            if (action != null) {
                Icon(
                    imageVector = AppIconKey.FORWARD.vector(),
                    contentDescription = null,
                    tint = AppColorKey.Icon.SECONDARY.color(),
                )
            }
        }
    }
}

/** The chips of a card; each sends its action. */
@Composable
internal fun DetailsChips(chips: List<DetailsChip>, onIntent: (SpaceDetailsIntent) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(AppDimenKey.Padding.SMALL.dp()),
        verticalArrangement = Arrangement.spacedBy(AppDimenKey.Padding.SMALL.dp()),
        modifier = Modifier.padding(AppDimenKey.Padding.MEDIUM.dp()),
    ) {
        chips.forEach { chip ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimenKey.Padding.EXTRA_SMALL.dp()),
                modifier = Modifier
                    .height(CHIP_HEIGHT)
                    .clip(CircleShape)
                    .background(AppColorKey.Container.ACCENT.color())
                    .clickable(role = Role.Button) { onIntent(chip.action) }
                    .padding(horizontal = AppDimenKey.Padding.MEDIUM.dp()),
            ) {
                Icon(
                    imageVector = chip.icon.vector(),
                    contentDescription = null,
                    tint = AppColorKey.Icon.ACCENT.color(),
                    modifier = Modifier.size(AppDimenKey.IconSize.SMALL.dp()),
                )
                Text(
                    text = chip.text,
                    style = AppTypographyKey.LABEL.style(),
                    color = AppColorKey.Text.PRIMARY.color(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private val CHIP_HEIGHT = 32.dp
