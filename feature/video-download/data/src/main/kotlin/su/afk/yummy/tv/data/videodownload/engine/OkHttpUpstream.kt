package su.afk.yummy.tv.data.videodownload.engine

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import su.afk.yummy.tv.core.utils.network.HttpHeaderNames
import su.afk.yummy.tv.core.utils.network.userAgentOrNull

/**
 * HTTP-источник загрузки на [client]: `User-Agent` из [headers] идёт как UA источника, остальные заголовки как
 * свойства запроса по умолчанию. Отдельная функция, чтобы заголовки на проводе проверялись тестом.
 */
@OptIn(UnstableApi::class)
internal fun okHttpUpstreamFactory(client: OkHttpClient, headers: Map<String, String>): DataSource.Factory {
    val requestHeaders = headers.filterKeys { !it.equals(HttpHeaderNames.USER_AGENT, ignoreCase = true) }
    return OkHttpDataSource.Factory(client).apply {
        headers.userAgentOrNull()?.let(::setUserAgent)
        if (requestHeaders.isNotEmpty()) setDefaultRequestProperties(requestHeaders)
    }
}
