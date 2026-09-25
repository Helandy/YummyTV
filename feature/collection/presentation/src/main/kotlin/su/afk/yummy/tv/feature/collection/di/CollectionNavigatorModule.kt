package su.afk.yummy.tv.feature.collection.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.collection.ICollectionNavigator
import su.afk.yummy.tv.feature.collection.navigator.CollectionNavigator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface CollectionNavigatorModule {

    @Binds
    @Singleton
    fun bindCollectionNavigator(impl: CollectionNavigator): ICollectionNavigator
}
