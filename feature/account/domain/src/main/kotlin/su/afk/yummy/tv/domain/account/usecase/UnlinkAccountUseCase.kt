package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.LinkedAccountProvider
import su.afk.yummy.tv.domain.account.repository.AccountRepository
import javax.inject.Inject

/** Отвязывает выбранного внешнего провайдера от текущего аккаунта. */
class UnlinkAccountUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    suspend operator fun invoke(provider: LinkedAccountProvider) =
        repository.unlinkAccount(provider)
}
