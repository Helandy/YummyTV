package su.afk.yummy.tv.core.storage.outbox.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.storage.db.AppDatabase
import su.afk.yummy.tv.core.model.mutation.PendingMutationQueue
import su.afk.yummy.tv.core.storage.outbox.OutboxPendingMutationQueue
import su.afk.yummy.tv.core.storage.outbox.PendingMutationOutbox
import su.afk.yummy.tv.core.storage.outbox.PendingMutationStore
import su.afk.yummy.tv.core.storage.outbox.PendingMutationSyncScheduler
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

    @Provides
    @Singleton
    internal fun providePendingMutationQueue(
        outbox: PendingMutationOutbox,
        scheduler: PendingMutationSyncScheduler,
    ): PendingMutationQueue = OutboxPendingMutationQueue(outbox, scheduler)
}
