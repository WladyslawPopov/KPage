package io.github.wladyslawpopov.kpage.demo.android.ui.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.color
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.style
import io.github.wladyslawpopov.kpage.demo.android.theme.vector
import io.github.wladyslawpopov.kpage.demo.presentation.component.ScreenError
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageEmptyState
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageIntent
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageToast
import io.github.wladyslawpopov.kpage.demo.presentation.page.ToastType
import io.github.wladyslawpopov.kpage.demo.shared.strings.asString
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppTypographyKey

/** A failure shown over the page without hiding it: close it, or retry when the page allows. */
@Composable
fun PageErrorBanner(
    error: ScreenError,
    canRetry: Boolean,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val failColor = AppColorKey.Status.FAIL.color()
    Surface(
        modifier = modifier.widthIn(max = BANNER_MAX_WIDTH).fillMaxWidth(),
        shape = RoundedCornerShape(AppDimenKey.CornerRadius.MEDIUM.dp()),
        color = AppColorKey.Container.ELEVATED.color(),
        border = BorderStroke(1.dp, failColor),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(AppIconKey.CLOSE.vector(), AppStringKey.Core.CLOSE.asString(), tint = failColor)
            }
            Text(
                text = error.text(),
                style = AppTypographyKey.BODY.style(),
                color = AppColorKey.Text.PRIMARY.color(),
                modifier = Modifier.weight(1f).padding(vertical = AppDimenKey.Padding.SMALL.dp()),
            )
            IconButton(onClick = onRetry, enabled = canRetry) {
                Icon(AppIconKey.REFRESH.vector(), AppStringKey.Core.RETRY.asString())
            }
        }
    }
}

/** The server's text when it gave one, else ours; the code is added for support. */
@Composable
private fun ScreenError.text(): String {
    val text = message?.takeIf { it.isNotBlank() } ?: title.asString()
    return if (code == null) text else "$text ($code)"
}

/** Shows the page toast and hides it with an animation; a tap dismisses it. */
@Composable
fun PageToastHost(toast: PageToast?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // The last toast stays drawn while it animates out after the page dropped it.
    val last = remember { LastToast() }
    if (toast != null) last.value = toast
    AnimatedVisibility(
        visible = toast != null,
        modifier = modifier,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
    ) {
        last.value?.let { shown ->
            Surface(
                onClick = onClick,
                shape = RoundedCornerShape(AppDimenKey.CornerRadius.MEDIUM.dp()),
                color = AppColorKey.Container.ELEVATED.color(),
                shadowElevation = 4.dp,
            ) {
                Row(
                    modifier = Modifier.padding(AppDimenKey.Padding.MEDIUM.dp()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimenKey.Padding.SMALL.dp()),
                ) {
                    Icon(shown.type.icon().vector(), null, tint = shown.type.tint().color())
                    Text(
                        text = shown.text.asString(),
                        style = AppTypographyKey.BODY.style(),
                        color = AppColorKey.Text.PRIMARY.color(),
                    )
                }
            }
        }
    }
}

// Not a state: writing it during composition must not trigger another one.
private class LastToast(var value: PageToast? = null)

private fun ToastType.icon(): AppIconKey = when (this) {
    ToastType.SUCCESS -> AppIconKey.SUCCESS
    ToastType.WARNING -> AppIconKey.WARNING
    ToastType.FAIL -> AppIconKey.FAIL
    ToastType.INFO -> AppIconKey.INFO
}

private fun ToastType.tint(): AppColorKey = when (this) {
    ToastType.SUCCESS -> AppColorKey.Status.SUCCESS
    ToastType.WARNING -> AppColorKey.Status.WARNING
    ToastType.FAIL -> AppColorKey.Status.FAIL
    ToastType.INFO -> AppColorKey.Icon.ACCENT
}

/**
 * Shown instead of the page body. Scrollable, so the page can still be pulled to refresh; the button
 * sends the action of [empty].
 */
@Composable
fun PageEmptyContent(
    empty: PageEmptyState,
    onAction: (PageIntent) -> Unit,
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(AppDimenKey.Padding.EXTRA_LARGE.dp()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimenKey.Padding.LARGE.dp()),
    ) {
        Icon(
            imageVector = empty.icon.vector(),
            contentDescription = null,
            tint = AppColorKey.Icon.SECONDARY.color(),
            modifier = Modifier.size(AppDimenKey.IconSize.EXTRA_LARGE.dp()),
        )
        Text(
            text = empty.title.asString(),
            style = AppTypographyKey.SUBTITLE.style(),
            color = AppColorKey.Text.PRIMARY.color(),
            textAlign = TextAlign.Center,
        )
        val action = empty.action
        val actionText = empty.actionText
        if (action != null && actionText != null) {
            Button(onClick = { onAction(action) }) { Text(actionText.asString()) }
        }
    }
}

private val BANNER_MAX_WIDTH = 560.dp
