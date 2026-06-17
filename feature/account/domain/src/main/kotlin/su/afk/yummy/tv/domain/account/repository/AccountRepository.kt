package su.afk.yummy.tv.domain.account.repository

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.account.model.AccountSession
import su.afk.yummy.tv.domain.account.model.YaniAccount

interface AccountRepository {
    suspend fun login(login: String, password: String, captchaResponse: String? = null): YaniAccount
    suspend fun refreshToken(): YaniAccount?
    fun observeSession(): Flow<AccountSession>
    suspend fun getSession(): AccountSession
    suspend fun getProfile(): YaniAccount
    suspend fun logout()
}
