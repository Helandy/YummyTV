package su.afk.yummy.tv.core.storage.watchlater.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.storage.db.AppDatabase
import su.afk.yummy.tv.core.storage.watchlater.WatchLaterStorage
import su.afk.yummy.tv.core.storage.watchlater.WatchLaterStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WatchLaterStorageModule {

    @Provides
    @Singleton
    internal fun provideWatchLaterStore(db: AppDatabase): WatchLaterStore =
        WatchLaterStore(db.watchLaterDao())

    @Provides
    @Singleton
    internal fun provideWatchLaterStorage(store: WatchLaterStore): WatchLaterStorage = store
}
