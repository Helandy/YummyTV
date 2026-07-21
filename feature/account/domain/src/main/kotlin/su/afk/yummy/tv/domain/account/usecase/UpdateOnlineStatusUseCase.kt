package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.AccountRepository
import javax.inject.Inject

/** Обновляет онлайн-статус авторизованного аккаунта для текущего устройства. */
class UpdateOnlineStatusUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    suspend operator fun invoke(deviceHash: String) {
        repository.updateOnlineStatus(deviceHash)
    }
}
