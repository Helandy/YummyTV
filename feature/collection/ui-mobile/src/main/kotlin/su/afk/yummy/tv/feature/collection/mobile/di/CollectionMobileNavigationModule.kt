package su.afk.yummy.tv.feature.collection.mobile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.collection.IMobileCollectionEntry
import su.afk.yummy.tv.feature.collection.mobile.navigator.CollectionNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface CollectionMobileNavigationModule {

    @Binds
    fun bindCollectionNavRegistrar(impl: CollectionNavRegistrar): IMobileCollectionEntry
}
