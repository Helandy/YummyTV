package su.afk.yummy.tv.feature.player.common

import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource

object PlayerDataSourceFactory {
    fun create(headers: Map<String, String>): DataSource.Factory =
        DefaultHttpDataSource.Factory().apply {
            setConnectTimeoutMs(CONNECT_TIMEOUT_MS)
            setReadTimeoutMs(READ_TIMEOUT_MS)
            headers.userAgent()?.takeIf { it.isNotBlank() }?.let(::setUserAgent)
            val requestHeaders =
                headers.filterKeys { !it.equals(USER_AGENT_HEADER, ignoreCase = true) }
            if (requestHeaders.isNotEmpty()) setDefaultRequestProperties(requestHeaders)
        }

    private fun Map<String, String>.userAgent(): String? =
        entries.firstOrNull { (key, _) -> key.equals(USER_AGENT_HEADER, ignoreCase = true) }?.value

    private const val USER_AGENT_HEADER = "User-Agent"

    // 2x от дефолтов DefaultHttpDataSource (8с): меньше ложных
    // ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT на медленных сетях/CDN.
    private const val CONNECT_TIMEOUT_MS = 16_000
    private const val READ_TIMEOUT_MS = 16_000
}
