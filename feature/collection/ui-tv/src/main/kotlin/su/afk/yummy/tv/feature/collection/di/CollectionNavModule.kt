package su.afk.yummy.tv.feature.collection.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.feature.collection.navigator.CollectionNavRegistrar
import su.afk.yummy.tv.feature.collection.navigator.CollectionNavigator
import su.afk.yummy.tv.feature.collection.ICollectionNavigator

@Module
@InstallIn(SingletonComponent::class)
interface CollectionNavModule {

    @Binds
    @IntoSet
    fun bindCollectionNavRegistrar(impl: CollectionNavRegistrar): NavRegistrar

    @Binds
    fun bindCollectionNavigator(impl: CollectionNavigator): ICollectionNavigator
}
