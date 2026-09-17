package su.afk.yummy.tv.domain.account.usecase

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.account.model.DiscoveredDevice
import su.afk.yummy.tv.domain.account.repository.LocalAuthRepository
import javax.inject.Inject

/**
 * Discovers TV devices on the local network for session transfer.
 */
class DiscoverLocalAuthDevicesUseCase @Inject constructor(
    private val repository: LocalAuthRepository,
) {
    operator fun invoke(): Flow<List<DiscoveredDevice>> = repository.discoverDevices()
}
