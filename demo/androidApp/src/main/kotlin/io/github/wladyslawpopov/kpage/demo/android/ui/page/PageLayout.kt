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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import io.github.wladyslawpopov.kpage.demo.android.theme.color
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.ui.LocalPageBottomPadding
import io.github.wladyslawpopov.kpage.demo.android.ui.LocalPageTopPadding
import io.github.wladyslawpopov.kpage.demo.android.ui.LocalTabBarScrollBehavior
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageIntent
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageState
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey

/**
 * The frame of every page: [header] on top with the error banner under it, the toast at the bottom,
 * and [body] or the empty state in between. The error never hides the body, so a cached list stays
 * readable. With refresh enabled the body can be pulled to send [PageIntent.Refresh].
 *
 * The body runs under the status bar, the translucent header and the tab bar; it gets both as padding and must
 * pass it to its list as content padding, not clip itself by it. It must be scrollable for the pull
 * to work.
 *
 * @param hideBottomBarOnScroll The page's scroll slides the tab bar away; pass true only when the
 * content is long enough, or a short list would hide the bar for nothing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageLayout(
    page: PageState,
    onPageIntent: (PageIntent) -> Unit,
    modifier: Modifier = Modifier,
    hideBottomBarOnScroll: Boolean = false,
    header: @Composable ColumnScope.() -> Unit = {},
    body: @Composable BoxScope.(PaddingValues) -> Unit,
) {
    val topPadding = LocalPageTopPadding.current
    val bottomPadding = LocalPageBottomPadding.current
    val tabBarScroll = LocalTabBarScrollBehavior.current

    // A page that is shown starts with the tab bar in place.
    LaunchedEffect(tabBarScroll, hideBottomBarOnScroll) {
        tabBarScroll?.state?.let {
            it.heightOffset = 0f
            it.contentOffset = 0f
        }
    }
    val scrollModifier = if (hideBottomBarOnScroll && tabBarScroll != null) {
        Modifier.nestedScroll(tabBarScroll.nestedScrollConnection)
    } else {
        Modifier
    }

    // A page inside this one gets the behavior only if this page does not use it.
    CompositionLocalProvider(
        LocalTabBarScrollBehavior provides if (hideBottomBarOnScroll) null else tabBarScroll,
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize().then(scrollModifier),
            containerColor = AppColorKey.Container.BACKGROUND.color(),
            contentWindowInsets = WindowInsets(0),
            topBar = {
                // Below the strip over the status bar; the body still runs under both.
                Column(Modifier.padding(top = topPadding)) {
                    header()
                    page.error?.let { error ->
                        PageErrorBanner(
                            error = error,
                            canRetry = !page.isLoading,
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
                    modifier = Modifier
                        .padding(bottom = bottomPadding)
                        .padding(AppDimenKey.Padding.MEDIUM.dp()),
                )
            },
        ) { padding ->
            val top = padding.calculateTopPadding()
            val bodyPadding = PaddingValues(top = top, bottom = bottomPadding)
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
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = top),
                        )
                    },
                ) { content() }
            } else {
                content()
            }
        }
    }
}
