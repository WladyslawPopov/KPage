package io.github.wladyslawpopov.kpage.demo.android.ui.screens.chat.room.photos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Turns the photos picked by the system into files of the app: scaled down to [MAX_SIDE], saved as
 * JPEG in the app's folder. Only the path crosses into the shared code, never the bytes.
 */
internal suspend fun savePhotos(context: Context, uris: List<Uri>): List<ChatAttachment> =
    withContext(Dispatchers.IO) {
        val folder = File(context.filesDir, FOLDER).apply { mkdirs() }
        uris.mapNotNull { uri -> savePhoto(context, uri, folder) }
    }

private fun savePhoto(context: Context, uri: Uri, folder: File): ChatAttachment? {
    val resolver = context.contentResolver
    // Reading the bounds alone returns no bitmap: the size is in the options.
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
    val decoded = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: return null

    val scale = minOf(1f, MAX_SIDE.toFloat() / maxOf(decoded.width, decoded.height))
    val bitmap = if (scale < 1f) {
        Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt(), (decoded.height * scale).toInt(), true)
    } else {
        decoded
    }
    val file = File(folder, "${UUID.randomUUID()}.jpg")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
    return ChatAttachment(path = file.absolutePath, width = bitmap.width, height = bitmap.height)
}

/** What an image view loads for a photo of a message. */
internal fun ChatAttachment.url(): String = "file://$path"

private const val FOLDER = "chat_photos"
private const val MAX_SIDE = 1600
private const val QUALITY = 80
