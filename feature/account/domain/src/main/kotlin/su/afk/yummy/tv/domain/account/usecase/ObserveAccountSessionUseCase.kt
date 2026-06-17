package su.afk.yummy.tv.domain.account.usecase

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.account.model.AccountSession
import su.afk.yummy.tv.domain.account.repository.AccountRepository
import javax.inject.Inject

/** Observes the current Yani authorization state without exposing stored credentials. */
class ObserveAccountSessionUseCase @Inject constructor(private val repository: AccountRepository) {
    operator fun invoke(): Flow<AccountSession> =
        repository.observeSession()
}
