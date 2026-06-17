package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.NotificationCount
import su.afk.yummy.tv.domain.account.repository.ProfileNotificationsRepository
import javax.inject.Inject

/** Загружает количество непрочитанных уведомлений по типам. */
class GetNotificationCountsUseCase @Inject constructor(private val repository: ProfileNotificationsRepository) {
    suspend operator fun invoke(): List<NotificationCount> =
        repository.getNotificationCounts()
}
