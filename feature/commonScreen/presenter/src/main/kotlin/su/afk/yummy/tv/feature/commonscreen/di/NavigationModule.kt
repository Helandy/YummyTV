package su.afk.yummy.tv.feature.commonscreen.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.error.api.ErrorDestinationFactory
import su.afk.yummy.tv.feature.commonscreen.errorScreen.ErrorNavigator
import su.afk.yummy.tv.feature.commonscreen.errorScreen.ErrorNavigatorRegister
import su.afk.yummy.tv.feature.commonscreen.navigator.IErrorScreenEntry
import su.afk.yummy.tv.feature.commonscreen.navigator.IImageViewEntry
import su.afk.yummy.tv.feature.commonscreen.navigator.IImageViewNavigator
import su.afk.yummy.tv.feature.commonscreen.navigator.ImageViewNavigator
import su.afk.yummy.tv.feature.commonscreen.navigator.ImageViewNavigatorRegister
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface NavigationModule {

    @Binds
    fun bindErrorNavigatorRegister(impl: ErrorNavigatorRegister): IErrorScreenEntry

    @Binds
    @Singleton
    fun bindErrorNavigator(impl: ErrorNavigator): ErrorDestinationFactory

    @Binds
    fun bindImageViewNavigatorRegister(impl: ImageViewNavigatorRegister): IImageViewEntry

    @Binds
    @Singleton
    fun bindImageViewNavigator(impl: ImageViewNavigator): IImageViewNavigator
}
