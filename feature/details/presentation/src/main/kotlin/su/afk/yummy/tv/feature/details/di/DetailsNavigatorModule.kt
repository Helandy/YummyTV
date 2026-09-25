package su.afk.yummy.tv.feature.details.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.core.deeplink.api.DeepLinkResolver
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.details.deeplink.DetailsDeepLinkResolver
import su.afk.yummy.tv.feature.details.navigator.DetailsNavigator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface DetailsNavigatorModule {

    @Binds
    @Singleton
    fun bindDetailsNavigator(impl: DetailsNavigator): IDetailsNavigator

    @Binds
    @IntoSet
    fun bindDetailsDeepLinkResolver(impl: DetailsDeepLinkResolver): DeepLinkResolver
}
