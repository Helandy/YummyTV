package su.afk.yummy.tv.feature.library.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.library.ILibraryNavigator
import su.afk.yummy.tv.feature.library.navigator.LibraryNavigator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface LibraryNavigatorModule {

    @Binds
    @Singleton
    fun bindLibraryNavigator(impl: LibraryNavigator): ILibraryNavigator
}
