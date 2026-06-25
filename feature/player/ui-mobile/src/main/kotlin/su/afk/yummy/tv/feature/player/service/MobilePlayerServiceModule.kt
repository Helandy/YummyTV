package su.afk.yummy.tv.feature.player.service

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface MobilePlayerServiceModule {

    @Binds
    @Singleton
    fun bindMobilePlayerPlaybackConfig(
        config: DefaultMobilePlayerPlaybackConfig,
    ): MobilePlayerPlaybackConfig
}
