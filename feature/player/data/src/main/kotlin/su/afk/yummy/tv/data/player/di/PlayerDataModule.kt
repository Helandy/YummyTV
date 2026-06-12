package su.afk.yummy.tv.data.player.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.data.player.repository.DefaultPlayerStreamRepository
import su.afk.yummy.tv.domain.player.repository.PlayerStreamRepository
import su.afk.yummy.tv.domain.player.usecase.ResolvePlayerStreamUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerDataModule {

    @Provides
    @Singleton
    fun providePlayerStreamRepository(
        repository: DefaultPlayerStreamRepository,
    ): PlayerStreamRepository = repository

    @Provides
    fun provideResolvePlayerStreamUseCase(
        repository: PlayerStreamRepository,
    ): ResolvePlayerStreamUseCase = ResolvePlayerStreamUseCase(repository)
}
