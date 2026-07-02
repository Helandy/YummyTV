package su.afk.yummy.tv.data.videodownload.worker

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import su.afk.yummy.tv.core.logger.AppLogger
import java.net.URI

internal const val VIDEO_DOWNLOAD_TAG = "VideoDownloadWorker"

internal fun logDownloadDebug(message: () -> String) {
    AppLogger.d(VIDEO_DOWNLOAD_TAG, message)
}

internal fun logDownloadInfo(message: () -> String) {
    AppLogger.i(VIDEO_DOWNLOAD_TAG, message)
}

internal fun logDownloadWarning(throwable: Throwable? = null, message: () -> String) {
    AppLogger.w(VIDEO_DOWNLOAD_TAG, throwable, message)
}

internal fun String.safeDownloadUrlForLog(): String =
    runCatching {
        val uri = URI(this)
        val fileName = uri.path
            ?.substringAfterLast('/')
            ?.takeIf { it.isNotBlank() }
        buildString {
            append(uri.scheme ?: "https")
            append("://")
            append(uri.host ?: this@safeDownloadUrlForLog.substringBefore('/'))
            if (fileName != null) {
                append("/.../")
                append(fileName)
            }
        }
    }.getOrElse {
        substringBefore('?').substringBefore('#')
    }

@OptIn(UnstableApi::class)
internal fun Throwable.downloadFailureDetails(): String {
    val httpError = findDownloadFailureCause<HttpDataSource.InvalidResponseCodeException>()
    return buildString {
        append(this@downloadFailureDetails::class.java.simpleName)
        message?.takeIf { it.isNotBlank() }?.let {
            append(": ")
            append(it)
        }
        if (httpError != null) {
            append("; http=")
            append(httpError.responseCode)
            httpError.responseMessage?.takeIf { it.isNotBlank() }?.let {
                append(" ")
                append(it)
            }
            append("; url=")
            append(httpError.dataSpec.uri.toString().safeDownloadUrlForLog())
            httpError.headerFields["content-type"]
                ?.firstOrNull()
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    append("; contentType=")
                    append(it)
                }
            httpError.headerFields.firstHeaderValue("x-vd")
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    append("; xVd=")
                    append(it)
                }
        }
    }
}

internal inline fun <reified T : Throwable> Throwable.findDownloadFailureCause(): T? {
    var current: Throwable? = this
    while (current != null) {
        if (current is T) return current
        current = current.cause
    }
    return null
}

private fun Map<String, List<String>>.firstHeaderValue(name: String): String? =
    entries.firstOrNull { (key, _) -> key.equals(name, ignoreCase = true) }
        ?.value
        ?.firstOrNull()
