package com.yunok.walzi.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer
import java.util.concurrent.ConcurrentHashMap

/**
 * Real download progress for image URLs a screen is watching. Fed by [ProgressInterceptor] on
 * Coil's OkHttp client. Only [track]ed URLs are reported, so grid thumbnails pay nothing for it.
 *
 * Value: null = not downloading (not started, served from cache, or finished);
 * [UNKNOWN] = downloading but the server sent no Content-Length; otherwise 0f..1f.
 */
object ImageLoadProgress {
    const val UNKNOWN = -1f

    private val tracked = ConcurrentHashMap<String, MutableStateFlow<Float?>>()

    /** Start watching [url]. Pair every call with [untrack] (e.g. in a DisposableEffect). */
    fun track(url: String): StateFlow<Float?> = tracked.getOrPut(url) { MutableStateFlow(null) }

    fun untrack(url: String) {
        tracked.remove(url)
    }

    internal fun progressFor(url: String): MutableStateFlow<Float?>? = tracked[url]
}

/** Wraps the response body of tracked URLs so bytes read are reported to [ImageLoadProgress]. */
class ProgressInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val progress = ImageLoadProgress.progressFor(request.url.toString()) ?: return response
        val body = response.body ?: return response
        return response.newBuilder().body(ProgressResponseBody(body, progress)).build()
    }
}

private class ProgressResponseBody(
    private val body: ResponseBody,
    private val progress: MutableStateFlow<Float?>
) : ResponseBody() {

    private val countingSource: BufferedSource by lazy { CountingSource(body.source()).buffer() }

    override fun contentType(): MediaType? = body.contentType()
    override fun contentLength(): Long = body.contentLength()
    override fun source(): BufferedSource = countingSource

    private inner class CountingSource(delegate: Source) : ForwardingSource(delegate) {
        private val total = body.contentLength()
        private var bytesRead = 0L
        private var lastPercent = -1

        init {
            progress.value = if (total > 0) 0f else ImageLoadProgress.UNKNOWN
        }

        override fun read(sink: Buffer, byteCount: Long): Long {
            val read = super.read(sink, byteCount)
            if (read == -1L) {
                progress.value = null
                return read
            }
            bytesRead += read
            if (total > 0) {
                // Emit only on whole-percent changes: at most ~100 updates per image.
                val percent = (bytesRead * 100 / total).toInt()
                if (percent != lastPercent) {
                    lastPercent = percent
                    progress.value = percent / 100f
                }
            }
            return read
        }

        override fun close() {
            super.close()
            progress.value = null
        }
    }
}
