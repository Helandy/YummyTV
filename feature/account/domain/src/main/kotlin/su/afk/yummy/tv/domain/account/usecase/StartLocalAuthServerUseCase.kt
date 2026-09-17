package su.afk.yummy.tv.domain.account.usecase

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.account.model.LocalAuthServerState
import su.afk.yummy.tv.domain.account.repository.LocalAuthRepository
import javax.inject.Inject

/**
 * Starts the local authentication server on TV.
 */
class StartLocalAuthServerUseCase @Inject constructor(
    private val repository: LocalAuthRepository,
) {
    operator fun invoke(): Flow<LocalAuthServerState> = repository.startServer()
}
