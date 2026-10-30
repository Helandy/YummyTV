package su.afk.yummy.tv.source.moon.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.source.moon.data.repository.MoonRepositoryImpl
import su.afk.yummy.tv.source.moon.domain.repository.MoonRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface MoonModule {

    @Binds
    @Singleton
    fun bindMoonRepository(impl: MoonRepositoryImpl): MoonRepository
}
