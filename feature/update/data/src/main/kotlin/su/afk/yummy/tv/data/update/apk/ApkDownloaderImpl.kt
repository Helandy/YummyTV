package su.afk.yummy.tv.data.update.apk

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.onDownload
import io.ktor.client.plugins.timeout
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.network.di.UnauthenticatedJsonClient
import su.afk.yummy.tv.data.update.R
import su.afk.yummy.tv.domain.update.repository.ApkDownloadRepository
import java.io.File
import java.io.IOException
import javax.inject.Inject

internal class ApkDownloaderImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @param:UnauthenticatedJsonClient private val httpClient: HttpClient,
) : ApkDownloadRepository {

    override suspend fun download(url: String, onProgress: suspend (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val apkFile = File(context.cacheDir, APK_FILE_NAME)
            val partFile = File(context.cacheDir, PART_FILE_NAME)
            apkFile.delete()
            partFile.delete()

            try {
                // prepareGet стримит тело: обычный get() целиком буферизовал бы APK в памяти.
                httpClient.prepareGet(url) {
                    timeout {
                        // Общий клиент ограничивает весь запрос 20 с, а длительность скачивания APK
                        // зависит от сети — ограничиваем только простой сокета, а не всю загрузку.
                        requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                        socketTimeoutMillis = DOWNLOAD_SOCKET_TIMEOUT_MS
                    }
                    onDownload { bytesSentTotal, contentLength ->
                        if (contentLength != null && contentLength > 0) {
                            onProgress((bytesSentTotal.toFloat() / contentLength).coerceIn(0f, 1f))
                        }
                    }
                }.execute { response ->
                    if (!response.status.isSuccess()) {
                        throw IOException(context.getString(R.string.update_download_error_http, response.status.value))
                    }

                    val written = response.bodyAsChannel().copyAndClose(partFile.writeChannel())

                    // Длина сжатого ответа не равна числу записанных байт, поэтому сверяем только без кодирования.
                    val expected = response.contentLength()
                    val isEncoded = response.headers[HttpHeaders.ContentEncoding] != null
                    if (!isEncoded && expected != null && expected > 0 && written != expected) {
                        throw IOException(
                            context.getString(R.string.update_download_error_incomplete, written, expected),
                        )
                    }
                }

                if (!partFile.isZipArchive()) {
                    throw IOException(context.getString(R.string.update_download_error_not_apk))
                }
                if (!partFile.renameTo(apkFile)) {
                    throw IOException(context.getString(R.string.update_download_error_save))
                }
            } finally {
                partFile.delete()
            }

            apkFile
        }

    private fun File.isZipArchive(): Boolean =
        inputStream().use { input ->
            val header = ByteArray(ZIP_MAGIC.size)
            input.read(header) == header.size && header.contentEquals(ZIP_MAGIC)
        }

    private companion object {
        const val APK_FILE_NAME = "update.apk"
        const val PART_FILE_NAME = "update.apk.part"
        const val DOWNLOAD_SOCKET_TIMEOUT_MS = 30_000L
        val ZIP_MAGIC = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
    }
}
