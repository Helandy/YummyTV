package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.UserDirectoryRepository
import javax.inject.Inject

/** Загружает профиль пользователя по его никнейму. */
class GetUserProfileByNicknameUseCase @Inject constructor(
    private val repository: UserDirectoryRepository,
) {
    suspend operator fun invoke(nickname: String) = repository.getProfileByNickname(nickname)
}
