package com.yunok.walzi.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Size
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ImageDownloader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** Saves the wallpaper into the shared Pictures/Walzi gallery folder via MediaStore. */
    suspend fun downloadToGallery(imageUrl: String, displayName: String): Result<Uri> =
        withContext(Dispatchers.IO) {
            try {
                // Same cached ImageLoader singleton as everywhere else in the app, and
                // Size.ORIGINAL so the saved file is always the full-resolution wallpaper,
                // never the smaller grid-thumbnail decode.
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .size(Size.ORIGINAL)
                    .allowHardware(false)
                    .build()
                val drawable = context.imageLoader.execute(request).drawable
                    ?: return@withContext Result.failure(IllegalStateException("Could not load image"))
                val bitmap = (drawable as? BitmapDrawable)?.bitmap
                    ?: return@withContext Result.failure(IllegalStateException("Unsupported image format"))

                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Walzi")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext Result.failure(IllegalStateException("Could not create gallery entry"))

                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                } ?: return@withContext Result.failure(IllegalStateException("Could not open output stream"))

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }

                Result.success(uri)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}