package io.github.wladyslawpopov.kpage.demo.android.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.AsyncImage
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import io.github.wladyslawpopov.kpage.demo.android.theme.color
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.vector
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey
import okio.Path.Companion.toOkioPath

/** The one image loader of the app: memory and disk caches shared by every image. */
fun createImageLoader(context: PlatformContext): ImageLoader =
    ImageLoader.Builder(context)
        .crossfade(true)
        .memoryCache { MemoryCache.Builder().maxSizePercent(context, MEMORY_SHARE).build() }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("images").toOkioPath())
                .maxSizeBytes(DISK_BYTES)
                .build()
        }
        .build()

/**
 * An image from the network over a quiet background. A missing [url] or a failed load shows the
 * "no image" icon. Light enough for lazy lists: no subcomposition per state.
 */
@Composable
fun RemoteImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    var failed by remember(url) { mutableStateOf(url == null) }
    Box(modifier.background(AppColorKey.Container.SECONDARY.color()), contentAlignment = Alignment.Center) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = contentScale,
                onSuccess = { failed = false },
                onError = { failed = true },
                modifier = Modifier.matchParentSize(),
            )
        }
        if (failed) {
            Icon(
                imageVector = AppIconKey.NO_IMAGE.vector(),
                contentDescription = null,
                tint = AppColorKey.Icon.SECONDARY.color(),
                modifier = Modifier.size(AppDimenKey.IconSize.LARGE.dp()),
            )
        }
    }
}

private const val MEMORY_SHARE = 0.25
private const val DISK_BYTES = 100L * 1024 * 1024
