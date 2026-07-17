package su.afk.yummy.tv.data.library.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.storage.library.LibraryStore
import su.afk.yummy.tv.data.library.repository.DefaultLibraryRepository
import su.afk.yummy.tv.data.library.repository.YaniWatchHistoryRepository
import su.afk.yummy.tv.domain.library.repository.LibraryRepository
import su.afk.yummy.tv.domain.library.repository.WatchHistoryRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LibraryDataModule {
    @Provides
    @Singleton
    fun provideLibraryRepository(store: LibraryStore): LibraryRepository =
        DefaultLibraryRepository(store)

    @Provides
    @Singleton
    fun provideWatchHistoryRepository(impl: YaniWatchHistoryRepository): WatchHistoryRepository =
        impl
}
