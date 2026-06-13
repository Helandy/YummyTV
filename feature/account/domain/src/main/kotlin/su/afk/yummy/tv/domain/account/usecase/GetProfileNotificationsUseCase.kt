package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.ProfileNotificationsRepository
import javax.inject.Inject

/** Loads a page of profile notifications for the signed-in Yani account. */
class GetProfileNotificationsUseCase @Inject constructor(private val repository: ProfileNotificationsRepository) {
    suspend operator fun invoke(limit: Int = 20, offset: Int = 0) = repository.getNotifications(limit, offset)
}
