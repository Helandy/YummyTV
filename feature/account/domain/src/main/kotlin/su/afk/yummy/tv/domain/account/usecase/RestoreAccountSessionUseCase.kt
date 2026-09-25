package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.YaniAccount
import su.afk.yummy.tv.domain.account.repository.AccountRepository
import javax.inject.Inject

/**
 * Восстанавливает вход в Yani из резервной копии токена, пережившей переустановку или перенос
 * на новое устройство. Ничего не делает, если сессия уже есть; возвращает профиль при успехе.
 */
class RestoreAccountSessionUseCase @Inject constructor(private val repository: AccountRepository) {
    suspend operator fun invoke(): YaniAccount? = repository.restoreSessionFromBackup()
}
