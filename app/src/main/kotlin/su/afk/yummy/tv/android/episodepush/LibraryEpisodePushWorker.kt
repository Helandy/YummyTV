package su.afk.yummy.tv.android.episodepush

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.preferences.settings.EpisodePushSettingsStore
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.domain.home.usecase.GetRecentlyAiredScheduleUseCase
import su.afk.yummy.tv.domain.home.usecase.ObserveLibraryNewEpisodeAnimeIdsUseCase
import su.afk.yummy.tv.domain.home.usecase.ObserveWatchedEpisodesUseCase
import su.afk.yummy.tv.domain.home.usecase.SelectLibraryEpisodePushesUseCase

/**
 * Локальный пуш о новых сериях тайтлов из списков пользователя — тот же расчёт, что у блока
 * «Новые серии из ваших списков» на главной: недавно вышедшие по расписанию × тайтлы из выбранных
 * в настройках списков − просмотренные серии. Серверная лента уведомлений не нужна: работает и
 * по тайтлам без подписки на озвучку. Что именно показывать, решает
 * [SelectLibraryEpisodePushesUseCase]; воркер только собирает данные и показывает пуши.
 * Учтённые серии запоминаются всегда, даже если пуш выключен или нет разрешения, иначе после
 * включения прилетел бы бэклог.
 */
@HiltWorker
class LibraryEpisodePushWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val settingsStore: SettingsStore,
    private val episodePushSettingsStore: EpisodePushSettingsStore,
    private val getRecentlyAiredSchedule: GetRecentlyAiredScheduleUseCase,
    private val observeLibraryNewEpisodeAnimeIds: ObserveLibraryNewEpisodeAnimeIdsUseCase,
    private val observeWatchedEpisodes: ObserveWatchedEpisodesUseCase,
    private val selectLibraryEpisodePushes: SelectLibraryEpisodePushesUseCase,
    private val notificationService: NewEpisodePushNotificationService,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runSuspendCatching {
        val nowSeconds = System.currentTimeMillis() / 1_000
        val libraryIds = observeLibraryNewEpisodeAnimeIds(settingsStore.newEpisodesSources.first()).first()
        val selection = selectLibraryEpisodePushes(
            recentlyAired = getRecentlyAiredSchedule(nowSeconds),
            libraryAnimeIds = libraryIds,
            watchedEpisodes = observeWatchedEpisodes().first(),
            knownKeys = episodePushSettingsStore.knownLibraryEpisodeKeys.first(),
            recentPushes = episodePushSettingsStore.recentPushes.first(),
            nowSeconds = nowSeconds,
        )
        val pushEnabled = episodePushSettingsStore.libraryEnabled.first() &&
            NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
        if (pushEnabled) {
            selection.pushes.forEach { push ->
                notificationService.showLibraryEpisode(push)
                episodePushSettingsStore.recordPush(push.animeId, nowSeconds)
            }
        }
        episodePushSettingsStore.setKnownLibraryEpisodeKeys(selection.knownKeys)
    }.fold(
        onSuccess = { Result.success() },
        onFailure = { Result.retry() },
    )
}
