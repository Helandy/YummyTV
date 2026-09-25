package su.afk.yummy.tv.feature.details.tv.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.details.ITvDetailsEntry
import su.afk.yummy.tv.feature.details.tv.navigator.DetailsNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface DetailsTvNavigationModule {

    @Binds
    fun bindDetailsNavRegistrar(impl: DetailsNavRegistrar): ITvDetailsEntry
}
