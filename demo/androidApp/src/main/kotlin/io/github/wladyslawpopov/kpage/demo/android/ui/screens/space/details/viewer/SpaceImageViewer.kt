package io.github.wladyslawpopov.kpage.demo.android.ui.screens.space.details.viewer

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.vector
import io.github.wladyslawpopov.kpage.demo.android.ui.widgets.BarIconButton
import io.github.wladyslawpopov.kpage.demo.android.ui.widgets.RemoteImage
import io.github.wladyslawpopov.kpage.demo.shared.strings.asString
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * The picture full screen on black: pinch or double tap zooms, a zoomed picture moves within its
 * edges. Closes by the cross, by back or by dragging the unzoomed picture down or up.
 */
@Composable
fun SpaceImageViewer(url: String, onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            ZoomableImage(url, onClose)
            BarIconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(AppDimenKey.Padding.SMALL.dp()),
            ) {
                Icon(AppIconKey.CLOSE.vector(), contentDescription = AppStringKey.Core.CLOSE.asString())
            }
        }
    }
}

@Composable
private fun ZoomableImage(url: String, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var size by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // A zoomed picture does not leave its edges; an unzoomed one moves only up and down, to be closed.
    fun bounded(value: Offset, zoom: Float): Offset {
        if (zoom <= 1f) return Offset(0f, value.y)
        val maxX = size.width * (zoom - 1) / 2
        val maxY = size.height * (zoom - 1) / 2
        return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
    }

    // Animates the picture back to its place, from where it is now; scale follows the same fraction.
    suspend fun animateTo(targetScale: Float) {
        val startScale = scale
        val startOffset = offset
        animate(0f, 1f) { fraction, _ ->
            scale = startScale + (targetScale - startScale) * fraction
            offset = startOffset * (1 - fraction)
        }
    }

    val transform = rememberTransformableState { _: Offset, zoomChange: Float, panChange: Offset, _: Float ->
        scale = (scale * zoomChange).coerceIn(MIN_ZOOM, MAX_ZOOM)
        offset = bounded(offset + panChange, scale)
    }
    // When the fingers are lifted: a long drag of the unzoomed picture closes it, a short one brings it back.
    LaunchedEffect(transform.isTransformInProgress) {
        if (transform.isTransformInProgress || scale > 1f) return@LaunchedEffect
        if (abs(offset.y) > size.height * CLOSE_DRAG_SHARE) onClose() else animateTo(1f)
    }

    RemoteImage(
        url = url,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        background = Color.Black,
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    scope.launch { animateTo(if (scale <= 1f) DOUBLE_TAP_ZOOM else 1f) }
                })
            }
            .transformable(transform)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
    )
}

private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f
private const val CLOSE_DRAG_SHARE = 0.15f
