package su.afk.yummy.tv.feature.details.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.domain.anime.IsAnimeRegionBlockedUseCase
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object DetailsPolicyModule {

    @Provides
    fun provideIsAnimeRegionBlockedUseCase(
        @Named("hideRegionBlocked") hideRegionBlocked: Boolean,
    ): IsAnimeRegionBlockedUseCase =
        IsAnimeRegionBlockedUseCase(
            hideRegionBlocked = hideRegionBlocked,
        )
}
