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
    val httpError = findDownloadFailureCause<HttpDataSource.HttpDataSourceException>()
        ?: return false
    return httpError.cause is SocketTimeoutException
}
