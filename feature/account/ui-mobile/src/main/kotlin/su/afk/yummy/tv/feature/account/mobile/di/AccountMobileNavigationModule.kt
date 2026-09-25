package su.afk.yummy.tv.feature.account.mobile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.account.IMobileAccountEntry
import su.afk.yummy.tv.feature.account.mobile.navigator.AccountNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface AccountMobileNavigationModule {

    @Binds
    fun bindAccountNavRegistrar(impl: AccountNavRegistrar): IMobileAccountEntry
}
