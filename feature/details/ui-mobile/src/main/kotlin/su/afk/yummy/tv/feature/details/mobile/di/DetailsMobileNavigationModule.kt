package su.afk.yummy.tv.feature.details.mobile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.details.IMobileDetailsEntry
import su.afk.yummy.tv.feature.details.mobile.navigator.DetailsNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface DetailsMobileNavigationModule {

    @Binds
    fun bindDetailsNavRegistrar(impl: DetailsNavRegistrar): IMobileDetailsEntry
}
