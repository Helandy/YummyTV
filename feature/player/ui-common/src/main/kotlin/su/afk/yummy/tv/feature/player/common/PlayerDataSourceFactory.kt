package su.afk.yummy.tv.feature.player.common

import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource

object PlayerDataSourceFactory {
    fun create(headers: Map<String, String>): DataSource.Factory =
        DefaultHttpDataSource.Factory().apply {
            headers.userAgent()?.takeIf { it.isNotBlank() }?.let(::setUserAgent)
            val requestHeaders =
                headers.filterKeys { !it.equals(USER_AGENT_HEADER, ignoreCase = true) }
            if (requestHeaders.isNotEmpty()) setDefaultRequestProperties(requestHeaders)
        }

    private fun Map<String, String>.userAgent(): String? =
        entries.firstOrNull { (key, _) -> key.equals(USER_AGENT_HEADER, ignoreCase = true) }?.value

    private const val USER_AGENT_HEADER = "User-Agent"
}
