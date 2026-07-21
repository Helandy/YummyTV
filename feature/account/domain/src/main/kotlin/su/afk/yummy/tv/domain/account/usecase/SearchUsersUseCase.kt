package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.UserDirectoryRepository
import javax.inject.Inject

/** Ищет пользователей по строке запроса с постраничной навигацией. */
class SearchUsersUseCase @Inject constructor(private val repository: UserDirectoryRepository) {
    suspend operator fun invoke(query: String, limit: Int, offset: Int) =
        repository.search(query, limit, offset)
}
