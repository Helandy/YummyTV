package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.ProfileNotificationsRepository

class MarkNotificationReadUseCase(private val repository: ProfileNotificationsRepository) {
    suspend operator fun invoke(id: Int) = repository.markNotificationRead(id)
}
