package su.afk.yummy.tv.feature.player.common.service

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface PlayerServiceModule {
    @Binds
    @Singleton
    fun bindPlayerPlaybackConfig(config: DefaultPlayerPlaybackConfig): PlayerPlaybackConfig
}
