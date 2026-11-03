package io.github.wladyslawpopov.kpage.demo.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import io.github.wladyslawpopov.kpage.demo.presentation.component.UiBinding

/**
 * Binds the screen UI to its component while this composable is in the composition and returns the
 * last rendered state; null until the first render.
 */
@Composable
fun <S : Any> rememberRenderedState(key: Any, bind: ((S) -> Unit) -> UiBinding): State<S?> {
    val state = remember(key) { mutableStateOf<S?>(null) }
    DisposableEffect(key) {
        val binding = bind { state.value = it }
        onDispose { binding.unbind() }
    }
    return state
}
