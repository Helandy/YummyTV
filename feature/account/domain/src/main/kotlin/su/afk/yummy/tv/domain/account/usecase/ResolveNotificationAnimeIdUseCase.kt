package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.ProfileNotificationsRepository

/** Resolves a notification anime slug into the canonical anime id. */
class ResolveNotificationAnimeIdUseCase(private val repository: ProfileNotificationsRepository) {
    suspend operator fun invoke(slug: String): Int? = repository.resolveAnimeIdBySlug(slug)
}
