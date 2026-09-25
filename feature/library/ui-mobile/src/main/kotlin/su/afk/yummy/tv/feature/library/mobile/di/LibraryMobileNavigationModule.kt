package su.afk.yummy.tv.feature.library.mobile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.library.IMobileLibraryEntry
import su.afk.yummy.tv.feature.library.mobile.navigator.LibraryNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface LibraryMobileNavigationModule {

    @Binds
    fun bindLibraryNavRegistrar(impl: LibraryNavRegistrar): IMobileLibraryEntry
}
