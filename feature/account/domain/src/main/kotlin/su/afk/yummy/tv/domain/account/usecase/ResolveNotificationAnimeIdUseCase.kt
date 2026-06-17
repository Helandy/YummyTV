package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.ProfileNotificationsRepository
import javax.inject.Inject

/** Преобразует slug аниме из уведомления в канонический идентификатор. */
class ResolveNotificationAnimeIdUseCase @Inject constructor(private val repository: ProfileNotificationsRepository) {
    suspend operator fun invoke(slug: String): Int? = repository.resolveAnimeIdBySlug(slug)
}
