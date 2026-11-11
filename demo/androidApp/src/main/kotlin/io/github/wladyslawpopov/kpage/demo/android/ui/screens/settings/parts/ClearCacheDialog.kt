package io.github.wladyslawpopov.kpage.demo.android.ui.screens.settings.parts

import androidx.compose.runtime.Composable
import io.github.wladyslawpopov.kpage.demo.android.ui.widgets.ConfirmDialog
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey

/** Asks whether to clear the cache of the paginators. */
@Composable
internal fun ClearCacheDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    ConfirmDialog(
        title = AppStringKey.Settings.CLEAR_CACHE_TITLE,
        message = AppStringKey.Settings.CLEAR_CACHE_MESSAGE,
        confirmText = AppStringKey.Settings.CLEAR_CACHE,
        onConfirm = onConfirm,
        onCancel = onCancel,
    )
}
