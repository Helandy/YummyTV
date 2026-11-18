package su.afk.yummy.tv.data.library.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.storage.library.LibraryStorage
import su.afk.yummy.tv.data.library.repository.DefaultAnimeListTransferRepository
import su.afk.yummy.tv.data.library.repository.DefaultLibraryRepository
import su.afk.yummy.tv.data.library.repository.YaniWatchHistoryRepository
import su.afk.yummy.tv.domain.library.repository.AnimeListTransferRepository
import su.afk.yummy.tv.domain.library.repository.LibraryRepository
import su.afk.yummy.tv.domain.library.repository.WatchHistoryRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LibraryDataModule {
    @Provides
    @Singleton
    fun provideLibraryRepository(store: LibraryStorage): LibraryRepository =
        DefaultLibraryRepository(store)

    @Provides
    @Singleton
    fun provideWatchHistoryRepository(impl: YaniWatchHistoryRepository): WatchHistoryRepository =
        impl

    @Provides
    @Singleton
    fun provideAnimeListTransferRepository(
        impl: DefaultAnimeListTransferRepository,
    ): AnimeListTransferRepository = impl
}
