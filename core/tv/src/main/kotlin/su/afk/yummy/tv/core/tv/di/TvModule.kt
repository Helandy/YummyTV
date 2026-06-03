package su.afk.yummy.tv.core.tv.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.tv.TvIntegration
import su.afk.yummy.tv.core.tv.api.ITvIntegration
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface TvModule {

    @Binds @Singleton
    fun bindTvIntegration(impl: TvIntegration): ITvIntegration
}
