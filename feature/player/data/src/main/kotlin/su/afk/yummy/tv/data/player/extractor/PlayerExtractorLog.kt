package su.afk.yummy.tv.data.player.extractor

import android.util.Log
import java.net.URI

private const val TAG = "PlayerExtractor"

internal fun logExtractorFailure(
    extractor: String,
    url: String,
    reason: String,
    throwable: Throwable? = null,
) {
    val message = "$extractor failed at ${url.safeUrlForLog()}: $reason"
    if (throwable == null) {
        Log.w(TAG, message)
    } else {
        Log.w(TAG, message, throwable)
    }
}

private fun String.safeUrlForLog(): String =
    runCatching {
        val uri = URI(this)
        buildString {
            append(uri.scheme ?: "https")
            append("://")
            append(uri.host ?: this@safeUrlForLog.substringBefore('/'))
            uri.path?.takeIf { it.isNotBlank() }?.let(::append)
        }
    }.getOrElse {
        substringBefore('?')
    }
