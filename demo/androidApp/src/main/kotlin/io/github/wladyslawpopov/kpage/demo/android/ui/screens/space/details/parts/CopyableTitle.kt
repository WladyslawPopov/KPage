package io.github.wladyslawpopov.kpage.demo.android.ui.screens.space.details.parts

import android.content.ClipData
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import io.github.wladyslawpopov.kpage.demo.android.theme.color
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.style
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppTypographyKey
import kotlinx.coroutines.launch

/**
 * The title: a long press copies it, with a short press-in of the text, a haptic tick and a
 * [onCopied] message (Android 13+ also shows its own clipboard preview).
 */
@Composable
internal fun CopyableTitle(title: String, onCopied: () -> Unit) {
    val clipboard = LocalClipboard.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    // Goes down on the copy and comes back at once: `pulse` is set, and reset when the move is over.
    var pulse by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pulse) PULSE_SCALE else 1f,
        animationSpec = spring(stiffness = 600f),
        finishedListener = { pulse = false },
        label = "titlePulse",
    )
    val highlight by animateColorAsState(
        targetValue = if (pulse) AppColorKey.Container.ACCENT.color() else Color.Transparent,
        label = "titleHighlight",
    )
    Text(
        text = title,
        style = AppTypographyKey.HEADLINE.style(),
        fontWeight = FontWeight.SemiBold,
        color = AppColorKey.Text.PRIMARY.color(),
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(AppDimenKey.CornerRadius.MEDIUM.dp()))
            .background(highlight)
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    pulse = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(title, title))) }
                    onCopied()
                },
            ),
    )
}

private const val PULSE_SCALE = 0.96f
