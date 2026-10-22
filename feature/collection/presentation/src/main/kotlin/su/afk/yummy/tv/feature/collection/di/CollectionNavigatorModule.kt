package su.afk.yummy.tv.feature.collection.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.core.deeplink.api.DeepLinkResolver
import su.afk.yummy.tv.feature.collection.ICollectionNavigator
import su.afk.yummy.tv.feature.collection.deeplink.CollectionDeepLinkResolver
import su.afk.yummy.tv.feature.collection.navigator.CollectionNavigator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface CollectionNavigatorModule {

    @Binds
    @Singleton
    fun bindCollectionNavigator(impl: CollectionNavigator): ICollectionNavigator

    @Binds
    @IntoSet
    fun bindCollectionDeepLinkResolver(impl: CollectionDeepLinkResolver): DeepLinkResolver
}
