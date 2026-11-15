package su.afk.yummy.tv.core.tv

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeFeedRefreshScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    fun schedule() {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            HOME_FEED_REFRESH_WORK_NAME,
            // UPDATE, а не KEEP: у поставленного раньше задания нет условия по заряду батареи.
            ExistingPeriodicWorkPolicy.UPDATE,
            createRefreshRequest(),
        )
    }

    private fun createRefreshRequest() =
        PeriodicWorkRequestBuilder<HomeFeedRefreshWorker>(
            repeatInterval = HOME_FEED_REFRESH_INTERVAL_HOURS,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
        )
            .setConstraints(networkConstraints())
            .setBackoffCriteria(
                backoffPolicy = BackoffPolicy.EXPONENTIAL,
                backoffDelay = HOME_FEED_REFRESH_BACKOFF_MINUTES,
                timeUnit = TimeUnit.MINUTES,
            )
            .build()

    // Фоновое освежение ленты и виджетов не срочное: при низком заряде пропускаем.
    private fun networkConstraints() =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

    private companion object {
        const val HOME_FEED_REFRESH_WORK_NAME = "home_feed_refresh"
        const val HOME_FEED_REFRESH_INTERVAL_HOURS = 6L
        const val HOME_FEED_REFRESH_BACKOFF_MINUTES = 30L
    }
}
