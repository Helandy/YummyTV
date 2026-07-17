package su.afk.yummy.tv.domain.account.repository

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.account.model.AccountSession
import su.afk.yummy.tv.domain.account.model.EditableProfile
import su.afk.yummy.tv.domain.account.model.LinkedAccountProvider
import su.afk.yummy.tv.domain.account.model.UserRegistration
import su.afk.yummy.tv.domain.account.model.YaniAccount

interface AccountRepository {
    suspend fun login(login: String, password: String, captchaResponse: String? = null): YaniAccount
    suspend fun register(registration: UserRegistration)
    suspend fun verifyRegistration(hash: String): YaniAccount
    suspend fun refreshToken(): YaniAccount?
    fun observeSession(): Flow<AccountSession>
    suspend fun getSession(): AccountSession
    suspend fun getProfile(): YaniAccount
    suspend fun refreshProfile(): EditableProfile
    suspend fun unlinkAccount(provider: LinkedAccountProvider): EditableProfile
    suspend fun updateOnlineStatus(deviceHash: String)
    suspend fun logout()
}
