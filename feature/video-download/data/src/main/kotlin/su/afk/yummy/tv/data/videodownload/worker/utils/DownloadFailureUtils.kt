package su.afk.yummy.tv.data.videodownload.worker.utils

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import su.afk.yummy.tv.core.common.player.isOkCdnHost
import su.afk.yummy.tv.data.videodownload.worker.findDownloadFailureCause
import java.net.SocketTimeoutException

@OptIn(UnstableApi::class)
internal fun Throwable.isForbiddenHttpResponse(): Boolean =
    findDownloadFailureCause<HttpDataSource.InvalidResponseCodeException>()?.responseCode == 403

/**
 * Узел okcdn отказал `400` на новое соединение при живой подписи. Это штатный сбой CVH: та же
 * подпись на резервном узле отдаётся (`docs/cvh-player.md`), поэтому загрузку надо повторить на
 * свежей ссылке, а не считать провалом.
 */
@OptIn(UnstableApi::class)
internal fun Throwable.isOkCdnNodeRefusal(): Boolean {
    val error = findDownloadFailureCause<HttpDataSource.InvalidResponseCodeException>() ?: return false
    return error.responseCode == 400 && error.dataSpec.uri.host?.isOkCdnHost() == true
}

@OptIn(UnstableApi::class)
internal fun Throwable.isTransientDownloadFailure(): Boolean {
    if (isOkCdnNodeRefusal()) return true
    if (findDownloadFailureCause<SocketTimeoutException>() != null) return true
    val httpError = findDownloadFailureCause<HttpDataSource.HttpDataSourceException>()
        ?: return false
    val responseCode = (httpError as? HttpDataSource.InvalidResponseCodeException)?.responseCode
    return responseCode in TRANSIENT_HTTP_RESPONSE_CODES
}

private val TRANSIENT_HTTP_RESPONSE_CODES = setOf(408, 429, 500, 502, 503, 504)
