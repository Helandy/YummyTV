package su.afk.yummy.tv.data.player.extractor

import su.afk.yummy.tv.core.logger.AppLogger
import java.net.URI

private const val TAG = "PlayerExtractor"

internal fun logExtractorFailure(
    extractor: String,
    url: String,
    reason: String,
    throwable: Throwable? = null,
) {
    AppLogger.w(TAG, throwable) {
        "$extractor failed at ${url.safeUrlForLog()}: $reason"
    }
}

private fun String.safeUrlForLog(): String =
    runCatching {
        val uri = URI(this)
        val fileName = uri.path
            ?.substringAfterLast('/')
            ?.takeIf { it.isNotBlank() }
        buildString {
            append(uri.scheme ?: "https")
            append("://")
            append(uri.host ?: this@safeUrlForLog.substringBefore('/'))
            if (fileName != null) {
                append("/.../")
                append(fileName)
            }
        }
    }.getOrElse {
        substringBefore('?').substringBefore('#')
    }
