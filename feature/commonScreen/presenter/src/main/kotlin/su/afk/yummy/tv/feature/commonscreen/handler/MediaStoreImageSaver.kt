package su.afk.yummy.tv.feature.commonscreen.handler

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import su.afk.yummy.tv.core.network.di.DownloadOkHttp
import java.io.File
import javax.inject.Inject

/**
 * Скачивает картинку напрямую по URL и сохраняет в `Pictures/YummyTV` через MediaStore:
 * на API 29+ без разрешений, на 24–28 пишет файл в публичную папку и регистрирует его в MediaStore.
 */
internal class MediaStoreImageSaver @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @DownloadOkHttp private val okHttpClient: OkHttpClient,
) : ImageSaver {

    override suspend fun save(url: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            okHttpClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
                check(response.isSuccessful) { "HTTP ${response.code}" }
                val body = checkNotNull(response.body) { "Empty body" }
                val mime = body.contentType()?.let { "${it.type}/${it.subtype}" }
                    ?.takeIf { it.startsWith("image/") } ?: DEFAULT_MIME
                val name = "yummytv_${System.currentTimeMillis()}.${extensionOf(mime)}"
                body.byteStream().use { input -> write(name, mime) { input.copyTo(it) } }
            }
        }
    }

    private fun write(name: String, mime: String, copy: (java.io.OutputStream) -> Unit) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, mime)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$FOLDER")
            values.put(MediaStore.Images.Media.IS_PENDING, 1)
            val uri = checkNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
            try {
                resolver.openOutputStream(uri).use { copy(checkNotNull(it)) }
                resolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                    null,
                    null,
                )
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
        } else {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), FOLDER)
            check(dir.exists() || dir.mkdirs()) { "Cannot create $dir" }
            val file = File(dir, name)
            file.outputStream().use(copy)
            @Suppress("DEPRECATION")
            values.put(MediaStore.Images.Media.DATA, file.absolutePath)
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        }
    }

    private fun extensionOf(mime: String): String = when (mime) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/avif" -> "avif"
        "image/gif" -> "gif"
        else -> "jpg"
    }

    private companion object {
        const val FOLDER = "YummyTV"
        const val DEFAULT_MIME = "image/jpeg"
    }
}
