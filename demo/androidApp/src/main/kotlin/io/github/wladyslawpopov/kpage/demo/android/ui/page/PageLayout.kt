package io.github.wladyslawpopov.kpage.demo.android.ui.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.wladyslawpopov.kpage.demo.android.theme.color
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageIntent
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageState
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey

/**
 * The frame of every page: [header] on top with the error banner under it, the toast at the bottom,
 * and [body] or the empty state in between. The error never hides the body, so a cached list stays
 * readable. With refresh enabled the body can be pulled to send [PageIntent.Refresh].
 *
 * [body] gets the padding of the header; it must be scrollable for the pull to work.
 */
@Composable
fun PageLayout(
    page: PageState,
    onPageIntent: (PageIntent) -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable ColumnScope.() -> Unit = {},
    body: @Composable BoxScope.(PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppColorKey.Container.BACKGROUND.color(),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column {
                header()
                page.error?.let { error ->
                    PageErrorBanner(
                        error = error,
                        canRetry = page.isRefreshEnabled && !page.isLoading,
                        onClose = { onPageIntent(PageIntent.ClearError) },
                        onRetry = { onPageIntent(PageIntent.Refresh) },
                        modifier = Modifier.padding(AppDimenKey.Padding.SMALL.dp()),
                    )
                }
            }
        },
        snackbarHost = {
            PageToastHost(
                toast = page.toast,
                onClick = { onPageIntent(PageIntent.DismissToast) },
                modifier = Modifier.padding(AppDimenKey.Padding.MEDIUM.dp()),
            )
        },
    ) { padding ->
        val bodyPadding = PaddingValues(top = padding.calculateTopPadding())
        val content: @Composable () -> Unit = {
            Box(Modifier.fillMaxSize()) {
                val empty = page.empty
                if (empty != null) {
                    PageEmptyContent(
                        empty = empty,
                        onAction = onPageIntent,
                        contentPadding = bodyPadding,
                    )
                } else {
                    body(bodyPadding)
                }
            }
        }
        if (page.isRefreshEnabled) {
            val pullState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = page.isLoading,
                onRefresh = { onPageIntent(PageIntent.Refresh) },
                state = pullState,
                modifier = Modifier.fillMaxSize(),
                // Below the header, not behind it.
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullState,
                        isRefreshing = page.isLoading,
                        modifier = Modifier.align(Alignment.TopCenter).padding(bodyPadding),
                    )
                },
            ) { content() }
        } else {
            content()
        }
    }
}
