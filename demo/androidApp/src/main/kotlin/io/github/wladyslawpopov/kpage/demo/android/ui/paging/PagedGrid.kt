package io.github.wladyslawpopov.kpage.demo.android.ui.paging

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import io.github.wladyslawpopov.kpage.core.paging.data.state.PagingRow
import io.github.wladyslawpopov.kpage.core.paging.data.state.PagingRows
import io.github.wladyslawpopov.kpage.demo.android.theme.color
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.style
import io.github.wladyslawpopov.kpage.demo.presentation.paginator.PaginatorUiState
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppTypographyKey

/**
 * The lazy grid of a paged list: one column is a list, more make a grid. It reports what is on
 * screen to the paginator through [TrackVisibleRows]; the paginator alone decides what to load.
 *
 * Row keys come from `PagingRows.keys`: the item's own key, with the position added only to an item
 * that briefly repeats after a server shift, so rows stay stable and keys never collide.
 *
 * @param keyOf Identity of an item; must be saveable in a Bundle (a string or a number).
 * @param decorationKeyOf Identity of a decoration placed before a server position; by default its
 * position and order. Must differ from the item keys.
 * @param reversed Starts the list from the bottom, as a chat does.
 * A scroll by the user hides the keyboard of a text field on the screen.
 *
 * @param showPosition Shows "last visible / total" over the bottom start corner.
 * @param spacing The gap between the cells, at the sides and above the first row.
 */
@Composable
fun <T : Any, D : Any> PagedGrid(
    list: PaginatorUiState<*, T, D>,
    keyOf: (T) -> Any,
    modifier: Modifier = Modifier,
    columns: Int = 1,
    reversed: Boolean = false,
    showPosition: Boolean = true,
    spacing: AppDimenKey.Padding = AppDimenKey.Padding.SMALL,
    gridState: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    decorationKeyOf: ((decoration: D, beforeIndex: Int) -> Any)? = null,
    decorationContent: @Composable (D) -> Unit = {},
    placeholderContent: @Composable (serverIndex: Int) -> Unit,
    itemContent: @Composable (item: T, serverIndex: Int) -> Unit,
) {
    val rows = list.rows
    val keys = remember(rows) { rows.keys(keyOf, decorationKeyOf) }

    list.events?.let { TrackVisibleRows(gridState, rows, it) }

    val gap = spacing.dp()
    val direction = LocalLayoutDirection.current
    val padding = PaddingValues(
        start = contentPadding.calculateStartPadding(direction) + gap,
        top = contentPadding.calculateTopPadding() + gap,
        end = contentPadding.calculateEndPadding(direction) + gap,
        bottom = contentPadding.calculateBottomPadding(),
    )

    // The user's scroll hides the keyboard, as in system apps; the typed text stays.
    val focusManager = LocalFocusManager.current
    val hideKeyboard = remember(focusManager) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) focusManager.clearFocus()
                return Offset.Zero
            }
        }
    }

    Box(modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = gridState,
            contentPadding = padding,
            reverseLayout = reversed,
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalArrangement = Arrangement.spacedBy(gap),
            modifier = Modifier.fillMaxSize().nestedScroll(hideKeyboard),
        ) {
            items(
                count = rows.rowCount,
                key = keys,
                // A row without a server position is a decoration: it takes the whole line.
                span = { index -> GridItemSpan(if (rows.serverIndexAt(index) == null) maxLineSpan else 1) },
            ) { index ->
                when (val row = rows[index]) {
                    is PagingRow.Item -> when (val item = row.item) {
                        null -> placeholderContent(row.serverIndex)
                        else -> itemContent(item, row.serverIndex)
                    }

                    is PagingRow.Decoration -> decorationContent(row.value)
                }
            }
        }

        if (showPosition) {
            PositionBadge(
                gridState = gridState,
                rows = rows,
                total = list.totalCount,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(contentPadding)
                    .padding(AppDimenKey.Padding.SMALL.dp()),
            )
        }
    }
}

/**
 * Whether the grid is long enough for its scroll to hide the bars: the content, without its padding,
 * is a quarter longer than the screen. The padding is left out so that hiding a bar, which changes
 * the padding, cannot change the answer.
 */
@Composable
fun rememberCanHideBars(gridState: LazyGridState): State<Boolean> = remember(gridState) {
    derivedStateOf {
        val indicator = gridState.scrollIndicatorState
        val layout = gridState.layoutInfo
        val viewport = indicator?.viewportSize ?: 0
        val content = (indicator?.contentSize ?: 0) - layout.beforeContentPadding - layout.afterContentPadding
        viewport > 0 && content.toLong() * 4 > viewport.toLong() * 5
    }
}

/** "Last visible / total" by server position; hidden while the list is empty. */
@Composable
private fun PositionBadge(
    gridState: LazyGridState,
    rows: PagingRows<*, *>,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val currentRows by rememberUpdatedState(rows)
    val position by remember(gridState) {
        derivedStateOf {
            val rows = currentRows
            gridState.layoutInfo.visibleItemsInfo
                .mapNotNull { info -> rows.serverIndexAt(info.index) }
                .maxOrNull()
                ?.plus(1)
        }
    }
    val shown = position ?: return
    if (total <= 0) return
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AppDimenKey.CornerRadius.FULL.dp()),
        color = AppColorKey.Container.ELEVATED.color(),
        shadowElevation = 2.dp,
    ) {
        Text(
            text = "${shown.coerceAtMost(total)} / $total",
            style = AppTypographyKey.CAPTION.style(),
            color = AppColorKey.Text.SECONDARY.color(),
            modifier = Modifier.padding(
                horizontal = AppDimenKey.Padding.MEDIUM.dp(),
                vertical = AppDimenKey.Padding.EXTRA_SMALL.dp(),
            ),
        )
    }
}
