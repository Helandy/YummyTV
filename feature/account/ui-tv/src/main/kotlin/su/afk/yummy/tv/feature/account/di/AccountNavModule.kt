package su.afk.yummy.tv.feature.account.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.account.navigator.AccountNavRegistrar
import su.afk.yummy.tv.feature.account.navigator.AccountNavigator

@Module
@InstallIn(SingletonComponent::class)
interface AccountNavModule {
    @Binds
    @IntoSet
    fun bindAccountNavRegistrar(impl: AccountNavRegistrar): NavRegistrar

    @Binds
    fun bindAccountNavigator(impl: AccountNavigator): IAccountNavigator
}
