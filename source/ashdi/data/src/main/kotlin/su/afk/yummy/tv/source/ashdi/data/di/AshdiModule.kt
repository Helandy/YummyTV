package su.afk.yummy.tv.source.ashdi.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.source.ashdi.data.repository.AshdiRepositoryImpl
import su.afk.yummy.tv.source.ashdi.domain.repository.AshdiRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AshdiModule {

    @Binds
    @Singleton
    fun bindAshdiRepository(impl: AshdiRepositoryImpl): AshdiRepository
}
