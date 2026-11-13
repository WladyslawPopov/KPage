package io.github.wladyslawpopov.kpage.demo.android.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.vector
import io.github.wladyslawpopov.kpage.demo.android.ui.barColor
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.AppTab
import io.github.wladyslawpopov.kpage.demo.shared.strings.asString
import io.github.wladyslawpopov.kpage.demo.uicore.testing.UiTestId
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey

/** The tabs of the app over a translucent background; only the selected tab shows its title. */
@Composable
fun TabBar(selected: AppTab, onSelect: (AppTab) -> Unit) {
    val corner = AppDimenKey.CornerRadius.MEDIUM.dp()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(barColor(), RoundedCornerShape(topStart = corner, topEnd = corner))
            // Taps between the items must not reach the content under the bar.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = { Icon(tab.icon.vector(), contentDescription = null) },
                label = { Text(tab.title.asString(), maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                alwaysShowLabel = false,
                modifier = Modifier.testTag(UiTestId.tab(tab.name)),
            )
        }
    }
}
