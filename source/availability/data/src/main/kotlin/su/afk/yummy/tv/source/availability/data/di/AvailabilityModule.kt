package su.afk.yummy.tv.source.availability.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.source.availability.data.repository.AvailabilityRepositoryImpl
import su.afk.yummy.tv.source.availability.domain.repository.AvailabilityRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AvailabilityModule {

    @Binds
    @Singleton
    fun bindAvailabilityRepository(impl: AvailabilityRepositoryImpl): AvailabilityRepository
}
