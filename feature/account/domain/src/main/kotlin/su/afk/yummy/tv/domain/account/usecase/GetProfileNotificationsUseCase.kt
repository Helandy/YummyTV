package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.ProfileNotificationsRepository
import javax.inject.Inject

/** Загружает страницу уведомлений профиля для авторизованного аккаунта Yani. */
class GetProfileNotificationsUseCase @Inject constructor(private val repository: ProfileNotificationsRepository) {
    suspend operator fun invoke(limit: Int = 20, offset: Int = 0) = repository.getNotifications(limit, offset)
}
