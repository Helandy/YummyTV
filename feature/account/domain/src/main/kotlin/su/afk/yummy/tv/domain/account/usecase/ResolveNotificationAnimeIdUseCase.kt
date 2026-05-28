package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.ProfileNotificationsRepository

class ResolveNotificationAnimeIdUseCase(private val repository: ProfileNotificationsRepository) {
    suspend operator fun invoke(slug: String): Int? = repository.resolveAnimeIdBySlug(slug)
}
