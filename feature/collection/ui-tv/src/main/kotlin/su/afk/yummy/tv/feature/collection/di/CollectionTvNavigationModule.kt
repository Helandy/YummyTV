package su.afk.yummy.tv.feature.collection.tv.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.collection.ITvCollectionEntry
import su.afk.yummy.tv.feature.collection.tv.navigator.CollectionNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface CollectionTvNavigationModule {

    @Binds
    fun bindCollectionNavRegistrar(impl: CollectionNavRegistrar): ITvCollectionEntry
}
