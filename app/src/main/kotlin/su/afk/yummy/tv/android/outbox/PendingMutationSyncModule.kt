package su.afk.yummy.tv.android.outbox

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.storage.outbox.PendingMutationSyncScheduler
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface PendingMutationSyncModule {

    @Binds
    @Singleton
    fun bindPendingMutationSyncScheduler(
        impl: AndroidPendingMutationSyncScheduler,
    ): PendingMutationSyncScheduler
}
