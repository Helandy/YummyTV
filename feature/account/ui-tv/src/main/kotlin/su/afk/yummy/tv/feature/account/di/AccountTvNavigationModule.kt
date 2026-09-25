package su.afk.yummy.tv.feature.account.tv.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.account.ITvAccountEntry
import su.afk.yummy.tv.feature.account.tv.navigator.AccountNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface AccountTvNavigationModule {

    @Binds
    fun bindAccountNavRegistrar(impl: AccountNavRegistrar): ITvAccountEntry
}
