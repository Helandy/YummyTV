package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.ProfileNotification
import su.afk.yummy.tv.domain.account.repository.ProfileNotificationsRepository
import javax.inject.Inject

/** Возвращает первую страницу уведомлений профиля из локального кэша без обращения к сети и проверки свежести. */
class GetCachedProfileNotificationsUseCase @Inject constructor(private val repository: ProfileNotificationsRepository) {
    suspend operator fun invoke(limit: Int = 20) = repository.getCachedNotifications(limit)
}
