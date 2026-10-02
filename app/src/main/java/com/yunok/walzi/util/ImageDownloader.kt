package com.yunok.walzi.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ImageDownloader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Saves the wallpaper into the shared Pictures/Walzi gallery folder via MediaStore.
     *
     * The original file is streamed straight to disk: no decode, no re-encode. That keeps the
     * saved file byte-identical to the source (no JPEG quality loss, PNG/WebP preserved) and
     * memory use flat regardless of image resolution.
     */
    suspend fun downloadToGallery(imageUrl: String, displayName: String): Result<Uri> =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            var pendingUri: Uri? = null
            try {
                client.newCall(Request.Builder().url(imageUrl).build()).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                    val body = response.body ?: throw IOException("Empty response")

                    val mime = body.contentType()
                        ?.let { "${it.type}/${it.subtype}" }
                        ?.takeIf { it.startsWith("image/") }
                        ?: "image/jpeg"
                    val extension = when (mime) {
                        "image/png" -> "png"
                        "image/webp" -> "webp"
                        else -> "jpg"
                    }

                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "${displayName.toSafeFileName()}.$extension")
                        put(MediaStore.Images.Media.MIME_TYPE, mime)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Walzi")
                            put(MediaStore.Images.Media.IS_PENDING, 1)
                        }
                    }

                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        ?: throw IOException("Could not create gallery entry")
                    pendingUri = uri

                    resolver.openOutputStream(uri)?.use { out -> body.byteStream().copyTo(out) }
                        ?: throw IOException("Could not open output stream")

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.clear()
                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(uri, values, null, null)
                    }
                    Result.success(uri)
                }
            } catch (e: CancellationException) {
                pendingUri.deleteQuietly()
                throw e
            } catch (e: Exception) {
                // Don't leave a half-written, invisible "pending" entry behind in the gallery.
                pendingUri.deleteQuietly()
                Result.failure(e)
            }
        }

    private fun Uri?.deleteQuietly() {
        if (this == null) return
        try {
            context.contentResolver.delete(this, null, null)
        } catch (_: Exception) {
        }
    }

    /** Titles come from a database - strip anything that isn't safe in a file name. */
    private fun String.toSafeFileName(): String =
        replace(Regex("[^\\p{L}\\p{N} _-]"), "").trim().ifEmpty { "walzi_wallpaper" }

    private companion object {
        val client: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        }
    }
}
