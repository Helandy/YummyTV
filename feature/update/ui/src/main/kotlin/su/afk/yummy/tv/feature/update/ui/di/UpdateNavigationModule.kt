package su.afk.yummy.tv.feature.update.ui.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.update.navigator.IUpdateEntry
import su.afk.yummy.tv.feature.update.ui.navigator.UpdateNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface UpdateNavigationModule {

    @Binds
    fun bindUpdateNavRegistrar(impl: UpdateNavRegistrar): IUpdateEntry
}
