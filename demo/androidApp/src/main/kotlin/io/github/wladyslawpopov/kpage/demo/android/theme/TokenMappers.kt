package io.github.wladyslawpopov.kpage.demo.android.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppTypographyKey

@Composable
fun AppColorKey.color(): Color {
    val scheme = MaterialTheme.colorScheme
    return when (this) {
        is AppColorKey.Text -> when (this) {
            AppColorKey.Text.PRIMARY -> scheme.onBackground
            // The primary text dimmed, as the secondary label on iOS; onSurfaceVariant is too close to it.
            AppColorKey.Text.SECONDARY -> scheme.onBackground.copy(alpha = SECONDARY_TEXT_ALPHA)
            AppColorKey.Text.MUTED -> scheme.outline
            AppColorKey.Text.ACCENT -> scheme.primary
            AppColorKey.Text.ON_ACCENT -> scheme.onPrimary
            AppColorKey.Text.THEME_1 -> scheme.tertiary
            AppColorKey.Text.THEME_2 -> scheme.secondary
        }

        is AppColorKey.Container -> when (this) {
            AppColorKey.Container.BACKGROUND -> scheme.background
            AppColorKey.Container.PRIMARY -> scheme.surface
            AppColorKey.Container.SECONDARY -> scheme.surfaceVariant
            AppColorKey.Container.ACCENT -> scheme.primaryContainer
            AppColorKey.Container.ELEVATED -> scheme.surfaceContainerHigh
            AppColorKey.Container.THEME_1 -> scheme.tertiaryContainer
            AppColorKey.Container.THEME_2 -> scheme.secondaryContainer
        }

        is AppColorKey.Status -> when (this) {
            AppColorKey.Status.SUCCESS -> Color(0xFF2E7D32)
            AppColorKey.Status.WARNING -> Color(0xFFF9A825)
            AppColorKey.Status.FAIL -> scheme.error
            AppColorKey.Status.NONE -> scheme.outline
        }

        is AppColorKey.Border -> when (this) {
            AppColorKey.Border.DEFAULT -> scheme.outlineVariant
            AppColorKey.Border.ACCENT -> scheme.primary
            AppColorKey.Border.FOCUSED -> scheme.primary
        }

        is AppColorKey.Icon -> when (this) {
            AppColorKey.Icon.PRIMARY -> scheme.onSurface
            AppColorKey.Icon.SECONDARY -> scheme.onSurfaceVariant
            AppColorKey.Icon.ACCENT -> scheme.primary
            AppColorKey.Icon.ON_ACCENT -> scheme.onPrimary
        }

        is AppColorKey.Overlay -> when (this) {
            AppColorKey.Overlay.SCRIM -> scheme.scrim
            AppColorKey.Overlay.DISABLED -> scheme.onSurface.copy(alpha = 0.38f)
        }
    }
}

fun AppDimenKey.dp(): Dp = when (this) {
    is AppDimenKey.Padding -> when (this) {
        AppDimenKey.Padding.NONE -> 0.dp
        AppDimenKey.Padding.EXTRA_SMALL -> 4.dp
        AppDimenKey.Padding.SMALL -> 8.dp
        AppDimenKey.Padding.MEDIUM -> 12.dp
        AppDimenKey.Padding.LARGE -> 16.dp
        AppDimenKey.Padding.EXTRA_LARGE -> 24.dp
    }

    is AppDimenKey.CornerRadius -> when (this) {
        AppDimenKey.CornerRadius.NONE -> 0.dp
        AppDimenKey.CornerRadius.SMALL -> 4.dp
        AppDimenKey.CornerRadius.MEDIUM -> 8.dp
        AppDimenKey.CornerRadius.LARGE -> 16.dp
        AppDimenKey.CornerRadius.FULL -> 999.dp
    }

    is AppDimenKey.IconSize -> when (this) {
        AppDimenKey.IconSize.EXTRA_SMALL -> 12.dp
        AppDimenKey.IconSize.SMALL -> 16.dp
        AppDimenKey.IconSize.MEDIUM -> 24.dp
        AppDimenKey.IconSize.LARGE -> 32.dp
        AppDimenKey.IconSize.EXTRA_LARGE -> 48.dp
    }
}

@Composable
fun AppTypographyKey.style(): TextStyle {
    val typography = MaterialTheme.typography
    return when (this) {
        AppTypographyKey.DISPLAY -> typography.displaySmall
        AppTypographyKey.HEADLINE -> typography.headlineSmall
        AppTypographyKey.TITLE -> typography.titleLarge
        AppTypographyKey.SUBTITLE -> typography.titleMedium
        AppTypographyKey.BODY -> typography.bodyMedium
        AppTypographyKey.LABEL -> typography.labelLarge
        AppTypographyKey.CAPTION -> typography.bodySmall
    }
}

fun AppIconKey.vector(): ImageVector = when (this) {
    AppIconKey.BACK -> Icons.AutoMirrored.Filled.ArrowBack
    AppIconKey.FORWARD -> Icons.AutoMirrored.Filled.KeyboardArrowRight
    AppIconKey.CLOSE -> Icons.Filled.Close
    AppIconKey.REFRESH -> Icons.Filled.Refresh
    AppIconKey.SUCCESS -> Icons.Filled.CheckCircle
    AppIconKey.WARNING -> Icons.Filled.Warning
    AppIconKey.FAIL -> Icons.Filled.Clear
    AppIconKey.INFO -> Icons.Filled.Info
    AppIconKey.NO_INTERNET -> Icons.Filled.Warning
    AppIconKey.NOTHING_FOUND -> Icons.Filled.Search
    AppIconKey.NO_IMAGE -> Icons.Filled.BrokenImage
    AppIconKey.SEARCH -> Icons.Filled.Search
    AppIconKey.FILTER -> Icons.Filled.FilterList
    AppIconKey.DROPDOWN -> Icons.Filled.ArrowDropDown
    AppIconKey.DONE -> Icons.Filled.Check
    AppIconKey.FAVORITE -> Icons.Filled.Favorite
    AppIconKey.FAVORITE_OFF -> Icons.Filled.FavoriteBorder
    AppIconKey.TRASH -> Icons.Filled.Delete
    AppIconKey.LAYOUT_LIST -> Icons.AutoMirrored.Filled.ViewList
    AppIconKey.LAYOUT_GRID -> Icons.Filled.GridView
    AppIconKey.AUTHOR -> Icons.Filled.Person
    AppIconKey.PHOTOGRAPHER -> Icons.Filled.PhotoCamera
    AppIconKey.LOCATION -> Icons.Filled.Place
    AppIconKey.KEYWORD -> Icons.Filled.Tag
    AppIconKey.ALBUM -> Icons.Filled.PhotoLibrary
    AppIconKey.CENTER -> Icons.Filled.Business
    AppIconKey.DATE -> Icons.Filled.CalendarToday
    AppIconKey.ADD -> Icons.Filled.Add
    AppIconKey.SEND -> Icons.AutoMirrored.Filled.Send
    AppIconKey.ATTACH -> Icons.Filled.AttachFile
    AppIconKey.COPY -> Icons.Filled.ContentCopy
    AppIconKey.MORE -> Icons.Filled.MoreVert
    AppIconKey.SPACE -> Icons.Filled.Star
    AppIconKey.CHAT -> Icons.Filled.Email
    AppIconKey.LAB -> Icons.Filled.Build
    AppIconKey.SETTINGS -> Icons.Filled.Settings
}

private const val SECONDARY_TEXT_ALPHA = 0.6f
