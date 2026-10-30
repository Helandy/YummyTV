package su.afk.yummy.tv.android.episodepush

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import su.afk.yummy.tv.domain.account.usecase.ObserveAccountSessionUseCase
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Планирует два фоновых задания пушей о новых сериях.
 *
 * Локальное ([LibraryEpisodePushWorker]) ставится всегда: гейт «включён ли пуш» стоит внутри
 * [LibraryEpisodePushWorker.doWork], само задание не переэнкьюивается.
 *
 * Профильное ([NewEpisodePushWorker]) нужно только авторизованному пользователю — без входа
 * ленты уведомлений нет, и холостой запуск раз в несколько часов ничего не даёт. Поэтому его
 * ставит и снимает [syncProfilePushWithSession] по состоянию сессии.
 */
@Singleton
class NewEpisodePushScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val observeAccountSession: ObserveAccountSessionUseCase,
) {

    fun scheduleLibraryPush() {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            LIBRARY_EPISODE_PUSH_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            createLibraryPushRequest(),
        )
    }

    /**
     * Держит профильное задание в соответствии со входом: ставит при авторизации и снимает при
     * выходе. Работает, пока жив вызывающий scope. UPDATE, а не KEEP: у тех, кто уже ставил
     * задание, оно было на 6 часов, и KEEP оставил бы старый интервал.
     */
    suspend fun syncProfilePushWithSession() {
        observeAccountSession()
            .map { session -> session.isAuthorized }
            .distinctUntilChanged()
            .collect { authorized ->
                val workManager = WorkManager.getInstance(context)
                if (authorized) {
                    workManager.enqueueUniquePeriodicWork(
                        EPISODE_PUSH_WORK_NAME,
                        ExistingPeriodicWorkPolicy.UPDATE,
                        createPushRequest(),
                    )
                } else {
                    workManager.cancelUniqueWork(EPISODE_PUSH_WORK_NAME)
                }
            }
    }

    private fun createPushRequest() =
        PeriodicWorkRequestBuilder<NewEpisodePushWorker>(
            repeatInterval = EPISODE_PUSH_INTERVAL_HOURS,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
        )
            .setConstraints(networkConstraints())
            .setBackoffCriteria(
                backoffPolicy = BackoffPolicy.EXPONENTIAL,
                backoffDelay = EPISODE_PUSH_BACKOFF_MINUTES,
                timeUnit = TimeUnit.MINUTES,
            )
            .build()

    private fun createLibraryPushRequest() =
        PeriodicWorkRequestBuilder<LibraryEpisodePushWorker>(
            repeatInterval = LIBRARY_EPISODE_PUSH_INTERVAL_HOURS,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
        )
            .setConstraints(networkConstraints())
            .setBackoffCriteria(
                backoffPolicy = BackoffPolicy.EXPONENTIAL,
                backoffDelay = EPISODE_PUSH_BACKOFF_MINUTES,
                timeUnit = TimeUnit.MINUTES,
            )
            .build()

    private fun networkConstraints() =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    private companion object {
        const val EPISODE_PUSH_WORK_NAME = "new_episode_push"
        const val LIBRARY_EPISODE_PUSH_WORK_NAME = "library_episode_push"
        const val EPISODE_PUSH_INTERVAL_HOURS = 3L
        const val LIBRARY_EPISODE_PUSH_INTERVAL_HOURS = 3L
        const val EPISODE_PUSH_BACKOFF_MINUTES = 30L
    }
}
