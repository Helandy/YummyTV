package su.afk.yummy.tv.source.fenix.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.source.fenix.data.repository.FenixRepositoryImpl
import su.afk.yummy.tv.source.fenix.domain.repository.FenixRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface FenixModule {

    @Binds
    @Singleton
    fun bindFenixRepository(impl: FenixRepositoryImpl): FenixRepository
}
