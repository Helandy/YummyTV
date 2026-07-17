package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.ProfileNotificationsRepository
import javax.inject.Inject

class DeleteAllNotificationsUseCase @Inject constructor(
    private val repository: ProfileNotificationsRepository,
) {
    suspend operator fun invoke(): Boolean = repository.deleteAllNotifications()
}
