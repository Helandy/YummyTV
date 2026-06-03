package su.afk.yummy.tv.feature.details.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.details.navigator.DetailsNavRegistrar
import su.afk.yummy.tv.feature.details.navigator.DetailsNavigator

@Module
@InstallIn(SingletonComponent::class)
interface DetailsNavModule {
    @Binds
    @IntoSet
    fun bindDetailsNavRegistrar(impl: DetailsNavRegistrar): NavRegistrar

    @Binds
    fun bindDetailsNavigator(impl: DetailsNavigator): IDetailsNavigator
}
