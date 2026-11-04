package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.repository.UserProfileRepository
import javax.inject.Inject

/** Возвращает сводку профиля пользователя Yani из локального кэша без обращения к сети и проверки свежести. */
class GetCachedUserProfileSummaryUseCase @Inject constructor(private val repository: UserProfileRepository) {
    suspend operator fun invoke(userId: Int) = repository.getCachedUserProfileSummary(userId)
}
