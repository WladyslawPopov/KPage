package io.github.wladyslawpopov.kpage.core.paging.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberConfigurationChangeCheck(): () -> Boolean {
    val context = LocalContext.current
    return remember(context) {
        { context.findActivity()?.isChangingConfigurations == true }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
