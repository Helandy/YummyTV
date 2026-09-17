package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.DiscoveredDevice
import su.afk.yummy.tv.domain.account.repository.LocalAuthRepository
import javax.inject.Inject

/**
 * Transfers the session to a TV device.
 */
class TransferSessionUseCase @Inject constructor(
    private val repository: LocalAuthRepository,
) {
    suspend operator fun invoke(device: DiscoveredDevice, pin: String, refreshToken: String) {
        repository.transferSession(device, pin, refreshToken)
    }
}
