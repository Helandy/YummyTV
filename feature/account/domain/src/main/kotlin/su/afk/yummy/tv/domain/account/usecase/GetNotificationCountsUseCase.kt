package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.ProfileNotificationsRepository

class GetNotificationCountsUseCase(private val repository: ProfileNotificationsRepository) {
    suspend operator fun invoke() = repository.getNotificationCounts()
}
