package su.afk.yummy.tv.feature.library.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.feature.library.ILibraryNavigator
import su.afk.yummy.tv.feature.library.navigator.LibraryNavRegistrar
import su.afk.yummy.tv.feature.library.navigator.LibraryNavigator

@Module
@InstallIn(SingletonComponent::class)
interface LibraryNavModule {
    @Binds
    @IntoSet
    fun bindLibraryNavRegistrar(impl: LibraryNavRegistrar): NavRegistrar

    @Binds
    fun bindLibraryNavigator(impl: LibraryNavigator): ILibraryNavigator
}
