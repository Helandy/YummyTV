package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.ProfileNotificationsRepository
import javax.inject.Inject

/** Загружает количество непрочитанных уведомлений по типам, считая их по флагу `viewed` в ленте (серверные счётчики включают и прочитанные). */
class GetNotificationCountsUseCase @Inject constructor(private val repository: ProfileNotificationsRepository) {
    suspend operator fun invoke() = repository.getNotificationCounts()
}
