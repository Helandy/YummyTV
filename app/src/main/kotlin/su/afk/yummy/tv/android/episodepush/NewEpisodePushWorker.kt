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
import su.afk.yummy.tv.core.common.widget.HomeScreenWidgetRefresher
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.core.preferences.settings.EpisodePushSettingsStore
import su.afk.yummy.tv.domain.account.model.ProfileNotification
import su.afk.yummy.tv.domain.account.model.unreadBadgeCount
import su.afk.yummy.tv.domain.account.usecase.GetProfileNotificationsUseCase
import su.afk.yummy.tv.domain.account.usecase.ResolveNotificationAnimeIdUseCase
import su.afk.yummy.tv.domain.home.usecase.SelectLibraryEpisodePushesUseCase

/**
 * Раз в 3 часа ([NewEpisodePushScheduler], только пока пользователь вошёл в аккаунт) опрашивает
 * ту же ленту уведомлений, что рисует вкладка «Уведомления» в аккаунте, и превращает новые
 * `isNewEpisode`-записи в системные пуши — даже если приложение не открывали. Этот же ответ
 * обновляет счётчик непрочитанных для бейджа аккаунта. Опрос идёт независимо от настроек пуша;
 * сам пуш показывается, только если он включён ([EpisodePushSettingsStore.enabled]) и выдано
 * системное разрешение.
 * Сервер сам решает, что считать новой серией по подписке ([ProfileNotification.isNewEpisode]),
 * поэтому воркеру не нужно ничего диффать самостоятельно.
 */
@HiltWorker
class NewEpisodePushWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val accountSettingsStore: YaniAccountSettingsStore,
    private val episodePushSettingsStore: EpisodePushSettingsStore,
    private val getProfileNotifications: GetProfileNotificationsUseCase,
    private val resolveNotificationAnimeId: ResolveNotificationAnimeIdUseCase,
    private val notificationService: NewEpisodePushNotificationService,
    private val widgetRefresher: HomeScreenWidgetRefresher,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val userId = accountSettingsStore.yaniUserId.first()
        if (userId <= 0) return Result.success()

        return runSuspendCatching {
            val notifications = getProfileNotifications(limit = NOTIFICATIONS_PAGE_SIZE)
            // Тот же запрос обслуживает бейдж непрочитанных на главной — отдельного опроса
            // счётчиков ради него больше нет.
            accountSettingsStore.setYaniUnreadNotificationsCount(notifications.unreadBadgeCount())
            val newEpisodeNotifications = notifications.filter { it.isNewEpisode }
            val knownIds = episodePushSettingsStore.knownNotificationIds.first()

            if (knownIds.isEmpty()) {
                // Первый прогон: фиксируем текущее состояние ленты как базовую линию, чтобы не
                // засыпать пользователя пушами про уже вышедшие серии.
                episodePushSettingsStore.addKnownNotificationIds(
                    newEpisodeNotifications.map(ProfileNotification::id).toSet(),
                )
                return@runSuspendCatching
            }

            val newOnes = newEpisodeNotifications.filterNot { it.id in knownIds }
            // Ленту опрашиваем всегда, но без разрешения пуш система всё равно выкинет — не тратим
            // сеть на resolveNotificationAnimeId. Id при этом помечаем известными, иначе после
            // выдачи разрешения прилетит бэклог.
            val pushEnabled = episodePushSettingsStore.enabled.first() &&
                NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
            if (pushEnabled) {
                newOnes.forEach { notification -> pushNotification(notification) }
            }
            episodePushSettingsStore.addKnownNotificationIds(
                newOnes.map(ProfileNotification::id).toSet(),
            )
            // Вышла серия — подписи карточек виджета («Серия N») могли устареть.
            if (newOnes.isNotEmpty()) widgetRefresher.refresh()
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
    }

    private suspend fun pushNotification(notification: ProfileNotification) {
        val animeId = notification.animeSlug?.let { slug -> resolveNotificationAnimeId(slug) }
        val nowSeconds = System.currentTimeMillis() / 1_000
        if (animeId != null) {
            // Локальный воркер мог уже сообщить об этой серии — один пуш на тайтл за сутки.
            val lastPush = episodePushSettingsStore.recentPushes.first()[animeId]
            if (lastPush != null && lastPush >= nowSeconds - SelectLibraryEpisodePushesUseCase.DEDUP_WINDOW_SECONDS) {
                return
            }
        }
        notificationService.showNewEpisode(notification, animeId)
        if (animeId != null) episodePushSettingsStore.recordPush(animeId, nowSeconds)
    }

    private companion object {
        const val NOTIFICATIONS_PAGE_SIZE = 30
    }
}
