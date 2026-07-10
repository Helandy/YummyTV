package su.afk.yummy.tv.data.videodownload.worker.utils

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import su.afk.yummy.tv.data.videodownload.worker.findDownloadFailureCause
import java.net.SocketTimeoutException

@OptIn(UnstableApi::class)
internal fun Throwable.isForbiddenHttpResponse(): Boolean =
    findDownloadFailureCause<HttpDataSource.InvalidResponseCodeException>()?.responseCode == 403

@OptIn(UnstableApi::class)
internal fun Throwable.isTransientDownloadFailure(): Boolean {
    if (findDownloadFailureCause<SocketTimeoutException>() != null) return true
    val httpError = findDownloadFailureCause<HttpDataSource.HttpDataSourceException>()
        ?: return false
    val responseCode = (httpError as? HttpDataSource.InvalidResponseCodeException)?.responseCode
    return responseCode in TRANSIENT_HTTP_RESPONSE_CODES
}

private val TRANSIENT_HTTP_RESPONSE_CODES = setOf(408, 429, 500, 502, 503, 504)
