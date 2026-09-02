package su.afk.yummy.tv.core.storage.outbox.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.storage.db.AppDatabase
import su.afk.yummy.tv.core.storage.outbox.PendingMutationOutbox
import su.afk.yummy.tv.core.storage.outbox.PendingMutationStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PendingMutationStorageModule {

    @Provides
    @Singleton
    internal fun providePendingMutationStore(db: AppDatabase): PendingMutationStore =
        PendingMutationStore(db.pendingMutationDao())

    @Provides
    @Singleton
    internal fun providePendingMutationOutbox(
        store: PendingMutationStore,
    ): PendingMutationOutbox = store
}
