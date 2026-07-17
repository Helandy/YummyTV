package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.AccountRepository
import javax.inject.Inject

class UpdateOnlineStatusUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    suspend operator fun invoke(deviceHash: String) {
        repository.updateOnlineStatus(deviceHash)
    }
}
