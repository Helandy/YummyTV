package su.afk.yummy.tv.feature.account.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.account.navigator.AccountNavigator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AccountNavigatorModule {

    @Binds
    @Singleton
    fun bindAccountNavigator(impl: AccountNavigator): IAccountNavigator
}
