package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.LocalAuthRepository
import javax.inject.Inject

/**
 * Stops the local authentication server on TV.
 */
class StopLocalAuthServerUseCase @Inject constructor(
    private val repository: LocalAuthRepository,
) {
    suspend operator fun invoke() = repository.stopServer()
}
