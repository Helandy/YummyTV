package su.afk.yummy.tv.feature.library.tv.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.library.ITvLibraryEntry
import su.afk.yummy.tv.feature.library.tv.navigator.LibraryNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface LibraryTvNavigationModule {

    @Binds
    fun bindLibraryNavRegistrar(impl: LibraryNavRegistrar): ITvLibraryEntry
}
