package su.afk.yummy.tv.domain.account.repository

import su.afk.yummy.tv.domain.account.model.YaniAccount

interface AccountRepository {
    suspend fun login(login: String, password: String, captchaResponse: String? = null): YaniAccount
    suspend fun refreshToken(): YaniAccount?
    suspend fun getProfile(): YaniAccount
    suspend fun logout()
}
