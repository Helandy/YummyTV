package su.afk.yummy.tv.android.outbox

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.core.storage.outbox.PendingMutationSyncScheduler
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [scheduleFlush] enqueues an immediate one-shot attempt (`NetworkType.CONNECTED` constraint means
 * `WorkManager` itself waits for a network if there isn't one yet); [schedule] additionally arms a
 * periodic safety net so a mutation queued while the process was killed still gets flushed.
 */
@Singleton
class AndroidPendingMutationSyncScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : PendingMutationSyncScheduler {

    fun schedule() {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<PendingMutationSyncWorker>(
                repeatInterval = PERIODIC_INTERVAL_HOURS,
                repeatIntervalTimeUnit = TimeUnit.HOURS,
            )
                .setConstraints(networkConstraints())
                .setBackoffCriteria(
                    backoffPolicy = BackoffPolicy.EXPONENTIAL,
                    backoffDelay = BACKOFF_MINUTES,
                    timeUnit = TimeUnit.MINUTES,
                )
                .build(),
        )
    }

    override fun scheduleFlush() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            FLUSH_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<PendingMutationSyncWorker>()
                .setConstraints(networkConstraints())
                .setBackoffCriteria(
                    backoffPolicy = BackoffPolicy.EXPONENTIAL,
                    backoffDelay = BACKOFF_MINUTES,
                    timeUnit = TimeUnit.MINUTES,
                )
                .build(),
        )
    }

    private fun networkConstraints() =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    private companion object {
        const val FLUSH_WORK_NAME = "pending_mutation_flush"
        const val PERIODIC_WORK_NAME = "pending_mutation_periodic_sync"
        const val PERIODIC_INTERVAL_HOURS = 6L
        const val BACKOFF_MINUTES = 15L
    }
}
