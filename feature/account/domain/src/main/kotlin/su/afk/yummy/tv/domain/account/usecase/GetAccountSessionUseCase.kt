package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.AccountSession
import su.afk.yummy.tv.domain.account.repository.AccountRepository
import javax.inject.Inject

/** Reads the current Yani authorization state without exposing stored credentials. */
class GetAccountSessionUseCase @Inject constructor(private val repository: AccountRepository) {
    suspend operator fun invoke(): AccountSession =
        repository.getSession()
}
