package su.afk.yummy.tv.feature.player.service

import androidx.media3.datasource.DataSource
import su.afk.yummy.tv.feature.player.common.PlayerDataSourceFactory
import javax.inject.Inject
import javax.inject.Singleton

internal interface MobilePlayerPlaybackConfig {
    fun updateStreamHeaders(headers: Map<String, String>)
    fun dataSourceFactory(): DataSource.Factory
}

@Singleton
internal class DefaultMobilePlayerPlaybackConfig @Inject constructor() :
    MobilePlayerPlaybackConfig {
    @Volatile
    private var streamHeaders: Map<String, String> = emptyMap()

    override fun updateStreamHeaders(headers: Map<String, String>) {
        streamHeaders = headers.toMap()
    }

    override fun dataSourceFactory(): DataSource.Factory =
        object : DataSource.Factory {
            override fun createDataSource(): DataSource =
                PlayerDataSourceFactory.create(streamHeaders).createDataSource()
        }
}
